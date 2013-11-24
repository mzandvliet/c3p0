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
	private final ISignal macdDiff;
	private final IWallet wallet;
	private final ITradeFloor tradeFloor;
	private final MacdTraderConfig config;
	private final long startDelay; // Used to avoid trading while macdBuffers are still empty and yield unstable signals
	
	private Sample lastDiff;
	
	private long lastBuyTime = 0l; // Last time we bought in milliseconds epoch
	private long lastSellTime = 0l; // Last time we sold in milliseconds epoch
	
	private long numSkippedTicks;
	
	private final List<ITradeListener> listeners;

	public MacdTraderNode(long timestep, ISignal macdDiff, IWallet wallet, ITradeFloor tradeFloor, MacdTraderConfig config, long startDelay) {
		super(timestep);
		this.macdDiff = macdDiff;
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
		Sample currentDiff = macdDiff.getSample(tick);
		
		if (numSkippedTicks > startDelay) {
			// We don't want to trade too often, check if we are not in the backoff period
			boolean buyBackOff = (lastBuyTime > tick - config.buyBackoffTimer);
			boolean sellBackOff = (lastSellTime > tick - config.sellBackoffTimer);
			
			if (!buyBackOff && currentDiff.value > config.minBuyDiffThreshold && wallet.getWalletUsd() > minDollars) {
				double dollars = wallet.getWalletUsd() * config.usdToBtcTradeAmount;
				double volumeBtc = tradeFloor.toBtc(dollars);
				
				TradeAction buyAction = new TradeAction(TradeActionType.BUY, tick, volumeBtc);
				double btcBought = tradeFloor.buy(wallet, buyAction);
				
				lastBuyTime = tick;
				
				notify(buyAction);
				//LOGGER.info(String.format("Bought %s BTC for %s USD because difference %s > %s", btcBought, dollars, currentDiff.value, config.minBuyDiffThreshold));
			}
			else if (!sellBackOff && currentDiff.value < config.minSellDiffThreshold && wallet.getWalletBtc() > tradeFloor.toBtc(minDollars)) {
				double btcToSell = wallet.getWalletBtc() * config.btcToUsdTradeAmount;
				TradeAction sellAction = new TradeAction(TradeActionType.SELL, tick, btcToSell);
				double soldForUSD = tradeFloor.sell(wallet, sellAction);
				
				lastSellTime = tick;
				
				notify(sellAction);
				//LOGGER.info(String.format("Sold %s BTC for %s USD because difference %s < %s", btcToSell, soldForUSD, currentDiff.value, config.minSellDiffThreshold));
			}
		}
		else {
			numSkippedTicks++;
		}
		
		lastDiff = currentDiff;
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
}
