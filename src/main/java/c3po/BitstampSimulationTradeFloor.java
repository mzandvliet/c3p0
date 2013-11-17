package c3po;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import c3po.TradeAction.TradeActionType;

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
	
	private double tradeFee = 0.05d;
	
	private double walletUsd;
	private double walletBtc;
	private List<TradeAction> actions;
	
	
	ISignal lastSignal;
	ISignal bidSignal;
	ISignal askSignal;
	
	public BitstampSimulationTradeFloor(ISignal last, ISignal bid, ISignal ask, double startDollars) {
		this.lastSignal = last;
		this.bidSignal = bid;
		this.askSignal = ask;
		this.walletUsd = startDollars;
		
		this.actions = new ArrayList<TradeAction>();
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
	public double buy(double volume) {
		// We get the latest ask, assuming the ticker is updated by some other part of the app
		Sample currentAsk = askSignal.peek();
		
		// The amount of Btc we are going to get if we buy for volume USD
		double buyBtc = volume * ((double) 1-tradeFee);
		
		// We assume the trade is fulfilled instantly, for the price of the ask
		walletUsd -= currentAsk.value * volume;
		
		// Add the bought volume to the wallet, minus the percentage from the tradefee
		walletBtc += buyBtc;
		
		actions.add(new TradeAction(TradeActionType.BUY, new Date().getTime(), volume));
		
		return buyBtc;
	}

	@Override
	public double sell(double volumeBitcoins) {
		// We get the latest ask, assuming the ticker is updated by some other part of the app
		Sample currentBid = bidSignal.peek();
		
		// We assume the trade is fulfilled instantly, for the price of the ask
		double soldForUsd = currentBid.value * (volumeBitcoins * (1-tradeFee));
		walletUsd += soldForUsd;
		walletBtc -= volumeBitcoins;
		
		actions.add(new TradeAction(TradeActionType.SELL, new Date().getTime(), volumeBitcoins));
		
		return soldForUsd;
	}

	@Override
	public List<TradeAction> getActions() {
		return actions;
	}
}
