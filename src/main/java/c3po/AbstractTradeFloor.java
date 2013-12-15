package c3po;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	
	public AbstractTradeFloor(ISignal last, ISignal bid, ISignal ask, boolean doLimitOrder) {
		this.tickerSignal = last;
		this.bidSignal = bid;
		this.askSignal = ask;
		
		this.tradeListeners = new ArrayList<ITradeListener>();
	}
	
	@Override
	public double toBtc(double usd) {
		return usd / tickerSignal.peek().value;
	}

	@Override
	public double toUsd(double btc) {
		return btc * tickerSignal.peek().value;
	}

	@Override
	public double getWalletValueInUsd(IWallet wallet) {
		return wallet.getWalletUsd() + toUsd(wallet.getWalletBtc());
	}

	@Override
	public double buy(IWallet wallet, TradeAction action) {
		// First call buyImpl, where the concrete class can store its buy logic
		double boughtBtc = buyImpl(wallet, action);
		
		// Notify what has happened
		notify(action);
		
		// Return the amount of Btc bought
		return boughtBtc;
	}
	
	/**
	 * Hook in buy() that can be implemented to add the actual TradeFloor logic.
	 * 
	 * @param wallet
	 * @param action
	 * @return Amount of BTC bought
	 */
	public abstract double buyImpl(IWallet wallet, TradeAction action);

	@Override
	public double sell(IWallet wallet, TradeAction action) {
		// First call sellImpl, where the concrete class can store its sell logic
		double boughtUsd = sellImpl(wallet, action);
		
		// Notify what has happened
		notify(action);
				
		// Return the amount of USD bought
		return boughtUsd;
	}
	
	/**
	 * Hook in sell() that can be implemented to add the actual TradeFloor logic.
	 * 
	 * @param wallet
	 * @param action
	 * @return Amount of USD bought
	 */
	public abstract double sellImpl(IWallet wallet, TradeAction action);

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
