package c3po.macd;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import c3po.AbstractTickable;
import c3po.ISignal;
import c3po.ITickable;
import c3po.ITradeActionSource;
import c3po.ITradeFloor;
import c3po.ITradeListener;
import c3po.IWallet;
import c3po.Sample;
import c3po.TradeAction;
import c3po.TradeAction.TradeActionType;

public class MacdTraderNode extends AbstractTickable implements ITickable, ITradeActionSource {
	private static final Logger LOGGER = LoggerFactory.getLogger(MacdTraderNode.class);
	private final ISignal buyMacdDiff;
	private final ISignal sellMacdDiff;
	private final IWallet wallet;
	private final ITradeFloor tradeFloor;
	private final MacdTraderConfig config;
	private final long startDelay; // Used to avoid trading while macdBuffers are still empty and yield unstable signals
	
	private long numSkippedTicks;
	private long lastBuyTimestamp = 0;
	private long lastSellTimestamp = 0;
	
	private boolean verbose = false;
	
	private final List<ITradeListener> listeners;
	private double lastBuyPrice = 0;
	private double lossCuttingPercentage = 0;

	public MacdTraderNode(long timestep, ISignal buyMacdDiff, ISignal sellMacdDiff, IWallet wallet, ITradeFloor tradeFloor, MacdTraderConfig config, long startDelay) {
		super(timestep);
		this.buyMacdDiff = buyMacdDiff;
		this.sellMacdDiff = sellMacdDiff;
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
	
	/*
	 *  Do trades purely based on zero-crossings in difference signal
	 */
	public void decide(long tick) {
		Sample buyCurrentDiff = buyMacdDiff.getSample(tick);
		Sample sellCurrentDiff = sellMacdDiff.getSample(tick);
		
		if (numSkippedTicks > startDelay) {
			boolean hasEnoughUsd = (wallet.getWalletUsd() / config.buyPercentage > minDollars);
			boolean hasEnoughBtc = (wallet.getWalletBtc() / config.sellPercentage > tradeFloor.toBtc(minDollars));
			boolean isAfterBuyBackoff = (tick > lastBuyTimestamp + config.buyBackoffTimer);
			boolean isAfterSellBackoff = (tick > lastSellTimestamp + config.sellBackoffTimer);		
			
			if(verbose)
				LOGGER.debug(String.format("Decide: hasEnoughUsd: %b, isAfterBuyBackoff: %b, hasEnoughBtc: %b, isAfterSellBackoff: %b", hasEnoughUsd, isAfterBuyBackoff, hasEnoughBtc, isAfterSellBackoff));
			
			if(hasEnoughUsd && isAfterBuyBackoff) {
				tryToOpenPosition(tick, buyCurrentDiff);
			}
			
			if(hasEnoughBtc && isAfterSellBackoff) {
				tryToClosePosition(tick, sellCurrentDiff);
			}	
			
			
			boolean hasBtc = (wallet.getWalletBtc() > tradeFloor.toBtc(minDollars));
			
			if(hasBtc) {
				tryLossSafeguard(tick);
			}
			
		}
		else {
			numSkippedTicks++;
		}
	}
	
	private void tryLossSafeguard(long tick) {

		boolean shouldSell = false;
		double peekBid = 0.0d;
		try {
			 peekBid = tradeFloor.peekBid();
			shouldSell = (lastBuyPrice != 0 && peekBid < lastBuyPrice * lossCuttingPercentage);
		} catch (Exception e) {
			LOGGER.error("Could not check for loss cutting safeguard", e);
		}
		
		if(shouldSell) {
			LOGGER.info(String.format("Performing loss cutting, because the current bid %,.2f is less then %,.2f of %,.2f", peekBid, lossCuttingPercentage, lastBuyPrice));
			double btcToSell = wallet.getWalletBtc(); // All-in
			TradeAction sellAction = new TradeAction(TradeActionType.SELL, tick, btcToSell);
			double usdReceived = tradeFloor.sell(wallet, sellAction);
			
			this.lastSellTimestamp = tick;
			
			notify(sellAction);
		}
	}

	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

	private void tryToOpenPosition(long tick, Sample currentDiff) {
		boolean buyThresholdReached = currentDiff.value > config.minBuyDiffThreshold;
		
		if(verbose)
			LOGGER.debug(String.format("%,.4f > %,.4f = %b", currentDiff.value, config.minBuyDiffThreshold, buyThresholdReached));

		if (buyThresholdReached) {
			double usdToSell = wallet.getWalletUsd() * config.buyPercentage;
			TradeAction buyAction = new TradeAction(TradeActionType.BUY, tick, usdToSell);
			double btcReceived = tradeFloor.buy(wallet, buyAction);

			this.lastBuyTimestamp = tick;
			this.lastBuyPrice = usdToSell / btcReceived;
			
			notify(buyAction);
		}
	}
	
	private void tryToClosePosition(long tick, Sample currentDiff) {
		boolean sellThresholdReached = currentDiff.value < config.minSellDiffThreshold;
		
		if(verbose)
			LOGGER.debug(String.format("%,.4f < %,.4f = %b", currentDiff.value, config.minSellDiffThreshold, sellThresholdReached));
		
		if (sellThresholdReached) {
			double btcToSell = wallet.getWalletBtc() * config.sellPercentage; 
			TradeAction sellAction = new TradeAction(TradeActionType.SELL, tick, btcToSell);
			double usdReceived = tradeFloor.sell(wallet, sellAction);
			
			this.lastSellTimestamp = tick;
			
			notify(sellAction);
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
