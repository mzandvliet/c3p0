package c3po.boundary;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import c3po.*;
import c3po.TradeAction.TradeActionType;
import c3po.macd.MacdAnalysisNode;
import c3po.macd.MacdTraderConfig;
import c3po.utils.SignalMath;
import c3po.utils.Time;
import c3po.wallet.IWallet;

public class BoundaryTraderNode extends AbstractTickable implements ITickable, ITradeActionSource {
	/**
	 * The percentage that the sell threshold will decrease, for every percent that the current price is
	 * higher then the previous buy.
	 */
	
	private static final Logger LOGGER = LoggerFactory.getLogger(BoundaryTraderNode.class);
	private final MacdAnalysisNode buyAnalysis;
	
	private final IWallet wallet;
	private final ITradeFloor tradeFloor;
	private final BoundaryTraderConfig config;
	private final long startDelay; // Used to avoid trading while macdBuffers are still empty and yield unstable signals
	
	private long numSkippedTicks;
	
	private boolean verbose = false;
	
	private final List<ITradeListener> listeners;

	private double lastHighestPositionPrice = -1; // TODO: managing this state explicitly is error prone
	private double lastBuyPrice = -1;
	private long lastSellTime = -1;
	private long sellTimeoutBeforeNextBuy = 10 * Time.MINUTES;

	public BoundaryTraderNode(long timestep, MacdAnalysisNode buyAnalysis, IWallet wallet, ITradeFloor tradeFloor, BoundaryTraderConfig config, long startDelay) {
		super(timestep);
		this.buyAnalysis = buyAnalysis;
		this.wallet = wallet;
		this.tradeFloor = tradeFloor;
		this.config = config;
		this.startDelay = startDelay;
		this.listeners = new ArrayList<ITradeListener>();
	}	

	public BoundaryTraderConfig getConfig() {
		return config;
	}
	
	@Override
	public void onNewTick(long tick) {
		decide(tick);
	}
	
	private final static double minDollars = 1.0d;
	
	public void decide(long tick) {
		Sample buyCurrentDiff = buyAnalysis.getOutputDifference().getSample(tick);
		
		if (numSkippedTicks > startDelay) {
			boolean hasEnoughUsd = wallet.getWalletUsd() > minDollars;
			boolean hasEnoughBtc = wallet.getWalletBtc() > tradeFloor.toBtc(minDollars);	
						
			if(hasEnoughUsd) {
				tryToOpenPosition(tick, buyCurrentDiff);
			}
			
			if(hasEnoughBtc) {
				tryClosePosition(tick);
			}			
		}
		else {
			numSkippedTicks++;
		}
	}
	
	private void tryClosePosition(long tick) {

		boolean shouldSell = false;
		double currentPrice = 0.0d;
		
		try {
			currentPrice = tradeFloor.toUsd(1d);
			
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
			this.lastSellTime = tick;
			
			notify(sellAction);
		}
	}

	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

	private void tryToOpenPosition(long tick, Sample currentDiff) {
		boolean buyThresholdReached = currentDiff.value > config.minBuyDiffThreshold;
		boolean sellTimeoutPassed = (lastSellTime <= 0 || tick > lastSellTime + sellTimeoutBeforeNextBuy);

		if (buyThresholdReached && sellTimeoutPassed) {
			double usdToSell = wallet.getWalletUsd();
			TradeAction buyAction = new TradeAction(TradeActionType.BUY, tick, usdToSell);
			double btcReceived = tradeFloor.buy(wallet, buyAction);

			// TODO: Get the buy price from the tradeFloor buy action instead
			double currentPrice = tradeFloor.toUsd(1d);
			this.lastBuyPrice = currentPrice;
			this.lastHighestPositionPrice = currentPrice;
			
			notify(buyAction);
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
