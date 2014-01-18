package c3po;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import c3po.macd.MacdTraderNode;
import c3po.structs.OpenOrder;
import c3po.utils.Time;
import c3po.wallet.IWallet;

/**
 * Bootstrap Tradefloor is an abstract implementation of the tradefloor that
 * helps reduce the boilerplate of concrete Tradefloors.
 * 
 * - Hides the concept of TradeListeners and their notifies
 */
public abstract class AbstractTradeFloor implements ITradeFloor {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractTradeFloor.class);
	
	private List<ITradeListener> tradeListeners;

	protected ISignal tickerSignal;
	protected ISignal bidSignal;
	protected ISignal askSignal;
	protected final boolean doLimitOrder = false;
	
	/**
	 * TradeTimeout makes sure we do not trade to quick in succession. Primary reason
	 * is currently that the bitstamp API takes a time to update it's wallet. If we
	 * trade too quick, we trade on unupdated wallet information which could lead
	 * to spending outside of our budget.
	 */
	private final long tradeTimout = 5 * Time.MINUTES;
	private double lastTradeTime = -1;
	
	public AbstractTradeFloor(ISignal last, ISignal bid, ISignal ask, boolean doLimitOrder) {
		this.tickerSignal = last;
		this.bidSignal = bid;
		this.askSignal = ask;
		
		this.tradeListeners = new ArrayList<ITradeListener>();
	}

	@Override
	public boolean allowedToTrade(long tick) {
		return lastTradeTime + tradeTimout <= tick;
	}

	@Override
	public double toBtc(long tick, double usd) {
		return usd / tickerSignal.getSample(tick).value;
	}

	@Override
	public double toUsd(long tick, double btc) {
		return btc * tickerSignal.getSample(tick).value;
	}

	@Override
	public double getWalletValueInUsd(long tick, IWallet wallet) {
		return wallet.getUsdTotal() + toUsd(tick, wallet.getBtcTotal());
	}

	@Override
	public OpenOrder buy(long tick, IWallet wallet, TradeAction action) {
		if (this.allowedToTrade(tick)) {
			// First call buyImpl, where the concrete class can store its buy logic
			OpenOrder order = buyImpl(tick, wallet, action);

			this.lastTradeTime = tick;

			// Notify what has happened
			notify(action);

			// Return the amount of Btc bought
			return order;
		} else {
			LOGGER.error("Not allowed to trade yet due to a recent trade");
			return null;
		}
	}
	
	/**
	 * Hook in buy() that can be implemented to add the actual TradeFloor logic.
	 * 
	 * @param wallet
	 * @param action
	 * @return Resulting Order
	 */
	protected abstract OpenOrder buyImpl(long tick, IWallet wallet, TradeAction action);

	@Override
	public OpenOrder sell(long tick, IWallet wallet, TradeAction action) {
		if(this.allowedToTrade(tick)) {
			// First call sellImpl, where the concrete class can store its sell logic
			OpenOrder order = sellImpl(tick, wallet, action);
			
			this.lastTradeTime = tick;
			
			// Notify what has happened
			notify(action);
					
			// Return the amount of USD bought
			return order;
		} else {	
			LOGGER.error("Not allowed to trade yet due to a recent trade");
			return null;
		}
	}
	
	/**
	 * Hook in sell() that can be implemented to add the actual TradeFloor logic.
	 * 
	 * @param wallet
	 * @param action
	 * @return Resulting Order
	 */
	protected abstract OpenOrder sellImpl(long tick, IWallet wallet, TradeAction action);

	private void notify(TradeAction action) {
		for (ITradeListener listener : tradeListeners) {
			listener.onTrade(action);
		}
	}

	@Override
	public void addTradeListener(ITradeListener listener) {
		tradeListeners.add(listener);
	}

	@Override
	public void removeListener(ITradeListener listener) {
		tradeListeners.remove(listener);
	}

	@Override
	public ITradeFloor getTradeFloor() {
		return this;
	}
	
	@Override
	public boolean doLimitOrder() {
		return doLimitOrder;
	}
	
	@Override
	public void adjustOrders() {}
}
