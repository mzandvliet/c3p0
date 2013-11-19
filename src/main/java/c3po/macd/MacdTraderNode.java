package c3po.macd;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import c3po.ISignal;
import c3po.ITickable;
import c3po.ITradeActionSource;
import c3po.ITradeFloor;
import c3po.ITradeListener;
import c3po.IWallet;
import c3po.Sample;
import c3po.TradeAction;
import c3po.TradeAction.TradeActionType;

public class MacdTraderNode implements ITickable, ITradeActionSource {
	private static final Logger LOGGER = LoggerFactory.getLogger(MacdTraderNode.class);
	private final ISignal macdDiff;
	private final IWallet wallet;
	private final ITradeFloor tradeFloor;
	private final MacdTraderConfig config;
	
	private long lastTick;
	private Sample lastDiff;
	
	private long lastBuy = 0l; // Last time we bought in milliseconds epoch
	private long lastSell = 0l; // Last time we sold in milliseconds epoch
	
	private long numSkippedTicks;
	
	private final List<ITradeListener> listeners;

	public MacdTraderNode(ISignal macdDiff, IWallet wallet, ITradeFloor tradeFloor, MacdTraderConfig config) {
		this.macdDiff = macdDiff;
		this.wallet = wallet;
		this.tradeFloor = tradeFloor;
		this.config = config;
		this.listeners = new ArrayList<ITradeListener>();
	}	

	public MacdTraderConfig getConfig() {
		return config;
	}
	
	@Override
	public long getLastTick() {
		return lastTick;
	}
	
	@Override
	public void tick(long tick) {
		if (tick > lastTick) {
			decide(tick);
		}
		lastTick = tick;
	}
	
	private final static double minDollars = 1.0d;
	
	/*
	 *  Do trades purely based on zero-crossings in difference signal
	 */
	public void decide(long tick) {
		Sample currentDiff = macdDiff.getSample(tick);
		
		if (numSkippedTicks > config.startDelay) {
			// We dont want to trade too often, check if we arent in the backoff period
			boolean buyInBackOffTimer = (lastBuy > tick - config.buyBackoffTimer);
			boolean sellInBackOffTimer = (lastSell > tick - config.sellBackoffTimer);
			if (!buyInBackOffTimer && currentDiff.value > config.minBuyDiffThreshold && wallet.getWalledUsd() > minDollars) {
				// Trade half of current usdWallet
				double dollars = wallet.getWalledUsd() * config.usdToBtcTradeAmount;
				double volume = tradeFloor.toBtc(dollars);
				
				TradeAction buyAction = new TradeAction(TradeActionType.BUY, tick, volume);
				double btcBought = tradeFloor.buy(wallet, buyAction);
				lastBuy = tick;
				
				notify(buyAction);
				//LOGGER.info(String.format("Bought %s BTC for %s USD because difference %s > %s", btcBought, dollars, currentDiff.value, config.minBuyDiffThreshold));
			}
			else if (!sellInBackOffTimer && currentDiff.value < config.minSellDiffThreshold && wallet.getWalletBtc() > tradeFloor.toBtc(minDollars)) {
				// Trade all of current btcWallet
				double btcToSell = wallet.getWalletBtc() * config.btcToUsdTradeAmount;
				TradeAction sellAction = new TradeAction(TradeActionType.SELL, tick, btcToSell);
				double soldForUSD = tradeFloor.sell(wallet, sellAction);
				lastSell = tick;
				
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
	public void addListener(ITradeListener listener) {
		listeners.add(listener);
	}

	@Override
	public void removeListener(ITradeListener listener) {
		listeners.remove(listener);
	}
}
