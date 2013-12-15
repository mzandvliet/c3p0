package c3po.macd;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import c3po.*;
import c3po.TradeAction.TradeActionType;
import c3po.utils.SignalMath;
import c3po.wallet.IWallet;

public class MacdTraderNode extends AbstractTickable implements ITickable, ITradeActionSource {
	/**
	 * The percentage that the sell threshold will decrease, for every percent that the current price is
	 * higher then the previous buy.
	 */
	
	private static final Logger LOGGER = LoggerFactory.getLogger(MacdTraderNode.class);
	private final MacdAnalysisNode buyAnalysis;
	private final MacdAnalysisNode sellAnalysis;
	private final IWallet wallet;
	private final ITradeFloor tradeFloor;
	private final MacdTraderConfig config;
	private final long startDelay; // Used to avoid trading while macdBuffers are still empty and yield unstable signals
	
	private long numSkippedTicks;
	
	private boolean verbose = false;
	
	private final List<ITradeListener> listeners;
	private double lastHighestPositionPrice = -1; // TODO: managing this state explicitly is error prone
	private double lastBuyPrice = -1;

	public MacdTraderNode(long timestep, MacdAnalysisNode buyAnalysis, MacdAnalysisNode sellAnalysis, IWallet wallet, ITradeFloor tradeFloor, MacdTraderConfig config, long startDelay) {
		super(timestep);
		this.buyAnalysis = buyAnalysis;
		this.sellAnalysis = sellAnalysis;
		this.wallet = wallet;
		this.tradeFloor = tradeFloor;
		this.config = config;
		this.startDelay = startDelay;
		this.listeners = new ArrayList<ITradeListener>();
	}	

	public MacdTraderConfig getConfig() {
		return config;
	}
	
	@Override
	public void onNewTick(long tick) {
		decide(tick);
	}
	
	private final static double minDollars = 1.0d;
	
	public void decide(long tick) {
		Sample buyCurrentDiff = buyAnalysis.getOutputDifference().getSample(tick);
		Sample sellCurrentDiff = sellAnalysis.getOutputDifference().getSample(tick);
		
		if (numSkippedTicks > startDelay) {
			boolean hasEnoughUsd = wallet.getWalletUsd() > minDollars;
			boolean hasEnoughBtc = wallet.getWalletBtc() > tradeFloor.toBtc(minDollars);	
			
//			if(verbose) {
//				LOGGER.debug(String.format("Decide: hasEnoughUsd: %b, isAfterBuyBackoff: %b, hasEnoughBtc: %b, isAfterSellBackoff: %b", hasEnoughUsd, isAfterBuyBackoff, hasEnoughBtc, isAfterSellBackoff));
//			}
			
			if(hasEnoughUsd) {
				tryToOpenPosition(tick, buyCurrentDiff);
			}
			
			if(hasEnoughBtc) {
				tryToClosePosition(tick, sellCurrentDiff);
				tryLossSafeguard(tick);
			}			
		}
		else {
			numSkippedTicks++;
		}
	}
	
	private void tryLossSafeguard(long tick) {

		boolean shouldSell = false;
		double currentPrice = 0.0d;
		
		try {
			currentPrice = sellAnalysis.getOutputFast().getSample(tick).value;
			
			if (currentPrice > this.lastHighestPositionPrice)
				this.lastHighestPositionPrice = currentPrice;
			
			shouldSell = (currentPrice < lastHighestPositionPrice * config.lossCutThreshold);
		} catch (Exception e) {
			LOGGER.error("Could not check for loss cutting safeguard", e);
		}
		
		if(shouldSell) {
			if (verbose)
				LOGGER.debug(String.format("Cutting at %s. Last buy: %s, because the current price %,.2f is less than %,.2f of %,.2f", new Date(tick), String.valueOf(lastBuyPrice), currentPrice, config.lossCutThreshold, lastHighestPositionPrice));
			
			double btcToSell = wallet.getWalletBtc(); // All-in
			TradeAction sellAction = new TradeAction(TradeActionType.SELL, tick, btcToSell);
			double usdReceived = tradeFloor.sell(wallet, sellAction);
			
			this.lastHighestPositionPrice = -1;

			notify(sellAction);
		}
	}

	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

	private void tryToOpenPosition(long tick, Sample currentDiff) {
		boolean buyThresholdReached = currentDiff.value > config.minBuyDiffThreshold;
		
//		if(verbose)
//			LOGGER.debug(String.format("%,.4f > %,.4f = %b", currentDiff.value, config.minBuyDiffThreshold, buyThresholdReached));

		if (buyThresholdReached) {
			double usdToSell = wallet.getWalletUsd();
			TradeAction buyAction = new TradeAction(TradeActionType.BUY, tick, usdToSell);
			double btcReceived = tradeFloor.buy(wallet, buyAction);

			// TODO: Get the buy price from the tradeFloor buy action instead
			double currentPrice = tradeFloor.toUsd(1d);
			this.lastBuyPrice = currentPrice;
			this.lastHighestPositionPrice = sellAnalysis.getOutputFast().getSample(tick).value;
			
			notify(buyAction);
		}
	}

	private void tryToClosePosition(long tick, Sample currentDiff) {
		
		double currentPrice = tradeFloor.toUsd(1d);
		double currentSellThreshold = calculateCurrentSellThreshold(config.minSellDiffThreshold, lastBuyPrice, currentPrice, config.sellThresholdRelaxationFactor);
		boolean sellThresholdReached = currentDiff.value < currentSellThreshold;
		
//		if(verbose)
//			LOGGER.debug(String.format("%,.4f < %,.4f = %b", currentDiff.value, config.minSellDiffThreshold, sellThresholdReached));
		
		if (sellThresholdReached) {
			double btcToSell = wallet.getWalletBtc(); 
			TradeAction sellAction = new TradeAction(TradeActionType.SELL, tick, btcToSell);
			double usdReceived = tradeFloor.sell(wallet, sellAction);
			
			if (verbose)
				LOGGER.debug("Closing at " + new Date(tick) + ". Last Buy: " + String.valueOf(lastBuyPrice) + ", Current Price: " + String.valueOf(currentPrice) + ", Current Diff: " + String.valueOf(currentDiff.value) + ", New Treshold: " + String.valueOf(currentSellThreshold));
			
			this.lastHighestPositionPrice = -1;

			notify(sellAction);
		}
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

	private void notify(TradeAction action) {
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
