package c3po.macd;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import c3po.*;
import c3po.db.DbOverrideModusChecker;
import c3po.node.ExpMovingAverageNode;
import c3po.node.INode;
import c3po.simulation.NoOpOverrideModusChecker;
import c3po.structs.TradeIntention;
import c3po.structs.TradeIntention.TradeActionType;
import c3po.structs.TradeResult;
import c3po.utils.SignalMath;
import c3po.utils.Time;
import c3po.wallet.IWallet;

public class MacdTraderNode extends AbstractTickable implements ITickable, ITradeActionSource {
	/**
	 * The percentage that the sell threshold will decrease, for every percent that the current price is
	 * higher then the previous buy.
	 */
	
	private static final Logger LOGGER = LoggerFactory.getLogger(MacdTraderNode.class);
	private final ISignal price;
	private final MacdAnalysisNode buyAnalysis;
	private final MacdAnalysisNode sellAnalysis;
	private final MacdAnalysisNode volumeAnalysis;
	private final IWallet wallet;
	private final ITradeFloor tradeFloor;
	private final MacdTraderConfig config;
	private final long startDelay; // Used to avoid trading while macdBuffers are still empty and yield unstable signals
	private IOverrideModusChecker overrideModusChecker;
	
	private long numSkippedTicks;
	
	private boolean verbose = false;
	
	private final List<ITradeListener> listeners;
	private double lastHighestPositionPrice = -1; // TODO: managing this state explicitly is error prone
	private double lastBuyPrice = -1;

	public MacdTraderNode(long timestep, ISignal price, MacdAnalysisNode buyAnalysis, MacdAnalysisNode sellAnalysis, MacdAnalysisNode volumeAnalysis, IWallet wallet, ITradeFloor tradeFloor, MacdTraderConfig config, long startDelay) {
		super(timestep);
		this.price = price;
		this.buyAnalysis = buyAnalysis;
		this.sellAnalysis = sellAnalysis;
		this.volumeAnalysis = volumeAnalysis;
		this.wallet = wallet;
		this.tradeFloor = tradeFloor;
		this.overrideModusChecker = new NoOpOverrideModusChecker();
		this.config = config;
		this.startDelay = startDelay;
		this.listeners = new ArrayList<ITradeListener>();
	}	
	
	public void setOverrideModusChecker(IOverrideModusChecker overrideModusChecker) {
		this.overrideModusChecker = overrideModusChecker;
	}

	public MacdTraderConfig getConfig() {
		return config;
	}
	
	@Override
	public void update(long tick) {
		decide(tick);
	}
	
	private final static double minDollars = 1.0d;
	
	public void decide(long tick) {
		buyAnalysis.tick(tick);
		sellAnalysis.tick(tick);
		volumeAnalysis.tick(tick);
		
		if (numSkippedTicks > startDelay) {
			boolean allowedToTrade = tradeFloor.allowedToTrade(tick) && overrideModusChecker.mayTrade();
			boolean hasEnoughUsd = wallet.getUsdAvailable() > minDollars;
			boolean hasEnoughBtc = wallet.getBtcAvailable() > tradeFloor.toBtc(tick, minDollars);	

			if(allowedToTrade && hasEnoughUsd) {
				tryToOpenPosition(tick);
			}
			
			if(allowedToTrade && hasEnoughBtc) {
				tryToClosePosition(tick);
				tryToCutPosition(tick);
			}		
			
		}
		else {
			numSkippedTicks++;
		}
	}
	
	private void tryToOpenPosition(long tick) {
		Sample buyCurrentDiff = buyAnalysis.getOutputDifference().getSample(tick);
		boolean buyThresholdReached = buyCurrentDiff.value > config.minBuyDiffThreshold;
		boolean volumeThresholdReached = volumeAnalysis.getOutputDifference().getSample(tick).value > config.buyVolumeThreshold;

		if ((buyThresholdReached && volumeThresholdReached && overrideModusChecker.mayBuy()) || overrideModusChecker.mayBuy()) {			
			double usdToSell = wallet.getUsdAvailable();
			TradeIntention buyAction = new TradeIntention(TradeActionType.BUY, tick, usdToSell);
			TradeResult tradeResult = tradeFloor.buy(tick, wallet, buyAction);
			
			if (verbose)
				LOGGER.debug("Opening at " + new Date(tick));
	

			//double currentAveragePrice = averagePrice.getOutput(0).getSample(tick).value;
			double lastBuyPrice = price.getSample(tick).value;
			this.lastBuyPrice = lastBuyPrice;
			this.lastHighestPositionPrice = lastBuyPrice;

			notify(tradeResult);
		}
	}

	private void tryToClosePosition(long tick) {
		Sample sellCurrentDiff = sellAnalysis.getOutputDifference().getSample(tick);
		double currentPrice = tradeFloor.toUsd(tick, 1d);
		double currentSellThreshold = calculateCurrentSellThreshold(config.minSellDiffThreshold, lastBuyPrice, currentPrice, config.sellThresholdRelaxationFactor);
		boolean sellThresholdReached = sellCurrentDiff.value < currentSellThreshold;

		if ((sellThresholdReached && overrideModusChecker.maySell()) || overrideModusChecker.mustSell()) {
			double btcToSell = wallet.getBtcAvailable(); 
			TradeIntention sellAction = new TradeIntention(TradeActionType.SELL, tick, btcToSell);
			TradeResult tradeResult = tradeFloor.sell(tick, wallet, sellAction);
			
			if (verbose)
				LOGGER.debug("Closing at " + new Date(tick) + ". Last Buy: " + String.valueOf(lastBuyPrice) + ", Current Price: " + String.valueOf(currentPrice) + ", Current Diff: " + String.valueOf(sellCurrentDiff.value) + ", New Treshold: " + String.valueOf(currentSellThreshold));
			
			this.lastBuyPrice = -1;
			this.lastHighestPositionPrice = -1;

			notify(tradeResult);
		}
	}
	
	private void tryToCutPosition(long tick) {

		boolean shouldSell = false;
		double currentPrice = 0.0d;
		
		try {
			currentPrice = price.getSample(tick).value;
			
			if (currentPrice > this.lastHighestPositionPrice)
				this.lastHighestPositionPrice = currentPrice;
			
			shouldSell = (currentPrice < lastHighestPositionPrice * config.lossCutThreshold);
		} catch (Exception e) {
			LOGGER.error("Could not check for loss cutting safeguard", e);
		}
		
		if(shouldSell && overrideModusChecker.maySell()) {
			if (verbose)
				LOGGER.debug(String.format("Cutting at %s. Last buy: %s, because the current price %,.2f is less than %,.2f of %,.2f", new Date(tick), String.valueOf(lastBuyPrice), currentPrice, config.lossCutThreshold, lastHighestPositionPrice));
			
			double btcToSell = wallet.getBtcAvailable(); // All-in
			TradeIntention sellAction = new TradeIntention(TradeActionType.SELL, tick, btcToSell);
			TradeResult tradeResult = tradeFloor.sell(tick, wallet, sellAction);
			
			this.lastBuyPrice = -1;
			this.lastHighestPositionPrice = -1;

			notify(tradeResult);
		}
	}

	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}
	
	/**
	 * This method can lessen the sell diff threshold if we currently already looking at a nice profit
	 * 
	 * @param baseSellDiffTreshold The base sell diff threshold
	 * @param lastBuyPrice The price that we bought
	 * @param currentPrice The current price of the stock
	 * @param multiplier The modifier percentage per profit percentage
	 * @return
	 */
	public static double  calculateCurrentSellThreshold(double baseSellDiffTreshold, double lastBuyPrice, double currentPrice, double multiplier) {
		if(lastBuyPrice > 0 && currentPrice > lastBuyPrice) {  
			double priceDifference = currentPrice - lastBuyPrice;
			
			// Multiplier is in range between 0 and 100
			multiplier = SignalMath.clamp(multiplier, 0d, 100d);
			
			double thresholdScalar = 1d - Math.min((priceDifference/lastBuyPrice) * multiplier, 1d);
			return baseSellDiffTreshold * thresholdScalar; 
		} else {
			return baseSellDiffTreshold;
		}
	}

	@Override
	public ITradeFloor getTradeFloor() {
		return tradeFloor;
	}

	private void notify(TradeResult action) {	
		for (ITradeListener listener : listeners) {
			listener.onTrade(action);
		}
	}

	@Override
	public void addTradeListener(ITradeListener listener) {
		listeners.add(listener);
	}

	@Override
	public void removeListener(ITradeListener listener) {
		listeners.remove(listener);
	}
	
	public enum TradePosition {
		OPEN,
		CLOSED
	}
}
