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
	
	private TradePosition state;
	
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
		this.state = TradePosition.CLOSED;
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
			switch (state) {
			case CLOSED:
				tryToOpenPosition(tick, currentDiff);
			case OPEN:
				tryToClosePosition(tick, currentDiff);
			default:
				break;
			}
		}
		else {
			numSkippedTicks++;
		}
	}

	private void tryToOpenPosition(long tick, Sample currentDiff) {
		boolean enoughDollarsToBuy = wallet.getWalletUsd() > minDollars;
		boolean buyThresholdReached = currentDiff.value > config.minBuyDiffThreshold;
		
		if (state == TradePosition.CLOSED && enoughDollarsToBuy && buyThresholdReached) {
			double usdToSell = wallet.getWalletUsd(); // All-in
			TradeAction buyAction = new TradeAction(TradeActionType.BUY, tick, usdToSell);
			double btcReceived = tradeFloor.buy(wallet, buyAction);

			notify(buyAction);
			//LOGGER.info(String.format("Bought %s BTC for %s USD because difference %s > %s", btcReceived, usdToSell, currentDiff.value, config.minBuyDiffThreshold));
			
			state = TradePosition.OPEN;
		}
	}
	
	private void tryToClosePosition(long tick, Sample currentDiff) {
		boolean enoughBtcToSell = wallet.getWalletBtc() > tradeFloor.toBtc(minDollars);
		boolean sellThresholdReached = currentDiff.value < config.minSellDiffThreshold;
		
		if (enoughBtcToSell && sellThresholdReached) {
			double btcToSell = wallet.getWalletBtc(); // All-in
			TradeAction sellAction = new TradeAction(TradeActionType.SELL, tick, btcToSell);
			double usdReceived = tradeFloor.sell(wallet, sellAction);
			
			notify(sellAction);
			//LOGGER.info(String.format("Sold %s BTC for %s USD because difference %s < %s", btcToSell, usdReceived, currentDiff.value, config.minSellDiffThreshold));
			
			state = TradePosition.CLOSED;
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
