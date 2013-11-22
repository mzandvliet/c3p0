package c3po;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import c3po.TradeAction.TradeActionType;
import c3po.macd.MacdTraderNode;

/* Todo:
 * 
 * - Create currency abstraction
 * 		- A wallet can have capital in many different currencies
 * 		- A TradeAction should indicate which currency pair was used
 * - Factor in costs
 * - Perhaps this should be a node
 * 		- So it can hook up to signals and have access to bid/ask data that way
 * 		- Removes the need for ISignal.peek()
 * 		- So it can produce results as signals (like wallet values)
 */

public class BitstampSimulationTradeFloor implements ITradeFloor {
	private static final Logger LOGGER = LoggerFactory.getLogger(BitstampSimulationTradeFloor.class);
	
	private List<ITradeListener> tradeListeners;

	private double tradeFee = 0.05d;
	
	ISignal lastSignal;
	ISignal bidSignal;
	ISignal askSignal;
	
	public BitstampSimulationTradeFloor(ISignal last, ISignal bid, ISignal ask) {
		this.lastSignal = last;
		this.bidSignal = bid;
		this.askSignal = ask;
		
		this.tradeListeners = new ArrayList<ITradeListener>();
	}
	
	@Override
	public double toBtc(double usd) {
		return usd / lastSignal.peek().value;
	}

	@Override
	public double toUsd(double btc) {
		return btc * lastSignal.peek().value;
	}

	@Override
	public double getWalletValueInUsd(IWallet wallet) {
		return wallet.getWalledUsd() + toUsd(wallet.getWalletBtc());
	}

	@Override
	public double buy(IWallet wallet, TradeAction action) {
		// We get the latest ask, assuming the ticker is updated by some other part of the app
		Sample currentAsk = askSignal.peek();
				
		// The amount of Btc we are going to get if we buy for volume USD
		double boughtBtc = action.volume * (1.0d-tradeFee);
		double soldUsd = action.volume * currentAsk.value;
		
		// We assume the trade is fulfilled instantly, for the price of the ask
		wallet.transact(action.timestamp, -soldUsd, boughtBtc);
		
		notify(action);
		
		return boughtBtc;
	}

	@Override
	public double sell(IWallet wallet, TradeAction action) {
		// We get the latest ask, assuming the ticker is updated by some other part of the app
		Sample currentBid = bidSignal.peek();
		
		// We assume the trade is fulfilled instantly, for the price of the ask
		double boughtUsd = currentBid.value * (action.volume * (1.0d-tradeFee)); // volume in bitcoins
		double soldBtc = action.volume;
		
		wallet.transact(action.timestamp, boughtUsd, -soldBtc);
		
		notify(action);
		
		return boughtUsd;
	}

	
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
}
