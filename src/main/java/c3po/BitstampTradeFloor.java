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

public class BitstampTradeFloor implements ITradeFloor {
	
	private double tradeFee = 0.02d;
	
	private double walletUsd;
	private double walletBtc;
	private List<TradeAction> actions;
	
	
	ISignal lastSignal;
	ISignal bidSignal;
	ISignal askSignal;
	
	public BitstampTradeFloor(ISignal last, ISignal bid, ISignal ask, double startDollars) {
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
	public void buy(double volume) {
		// We get the latest ask, assuming the ticker is updated by some other part of the app
		Sample currentAsk = askSignal.peek();
		
		// We assume the trade is fulfilled instantly, for the price of the ask
		walletUsd -= currentAsk.value * volume;
		
		// Add the bought volume to the wallet, minus the percentage from the tradefee
		walletBtc += volume * ((double) 1-tradeFee);
		
		actions.add(new TradeAction(TradeActionType.BUY, new Date().getTime(), volume));
	}

	@Override
	public void sell(double volume) {
		// We get the latest ask, assuming the ticker is updated by some other part of the app
		Sample currentBid = bidSignal.peek();
		
		// We assume the trade is fulfilled instantly, for the price of the ask
		walletUsd += currentBid.value * volume;
		walletBtc -= volume * ((double) 1 + tradeFee);
		
		actions.add(new TradeAction(TradeActionType.SELL, new Date().getTime(), volume));
	}

	@Override
	public List<TradeAction> getActions() {
		return actions;
	}
}
