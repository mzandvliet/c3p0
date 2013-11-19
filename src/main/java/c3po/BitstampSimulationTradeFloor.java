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
	
	private List<ITradeListener> listeners;
	private double tradeFee = 0.05d;
	
	private double walletUsd;
	private double walletBtc;
	
	ISignal lastSignal;
	ISignal bidSignal;
	ISignal askSignal;
	
	public BitstampSimulationTradeFloor(ISignal last, ISignal bid, ISignal ask, double startDollars, double startBtc) {
		this.lastSignal = last;
		this.bidSignal = bid;
		this.askSignal = ask;
		this.walletUsd = startDollars;
		this.walletBtc = startBtc;
		
		this.listeners = new ArrayList<ITradeListener>();
	}
	
	@Override
	public double getWalledUsd() {
		return walletUsd;
	}

	@Override
	public double getWalletBtc() {
		return walletBtc;
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
	public double getWalletValue() {
		return walletUsd + toUsd(walletBtc);
	}

	@Override
	public double buy(TradeAction action) {
		// We get the latest ask, assuming the ticker is updated by some other part of the app
		Sample currentAsk = askSignal.peek();
				
		// The amount of Btc we are going to get if we buy for volume USD
		double buyBtc = action.volume * ((double) 1-tradeFee);
				
		// We assume the trade is fulfilled instantly, for the price of the ask
		walletUsd -= currentAsk.value * action.volume;
		
		// Add the bought volume to the wallet, minus the percentage from the tradefee
		walletBtc += buyBtc;
		
		notify(action);
		
		return buyBtc;
	}

	@Override
	public double sell(TradeAction action) {
		// We get the latest ask, assuming the ticker is updated by some other part of the app
		Sample currentBid = bidSignal.peek();
		
		// We assume the trade is fulfilled instantly, for the price of the ask
		double soldForUsd = currentBid.value * (action.volume * (1-tradeFee)); // volume in bitcoins, yo
		walletUsd += soldForUsd;
		walletBtc -= action.volume;
		
		notify(action);
		
		return soldForUsd;
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
	
	

	@Override
	public ITradeFloor getTradeFloor() {
		return this;
	}

	@Override
	public String toString() {
		return "Wallet: " + walletUsd + " USD, " + walletBtc + " BTC";
	}
}
