package c3po.bitstamp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import c3po.*;

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
public class BitstampSimulationTradeFloor extends AbstractTradeFloor {
	private static final Logger LOGGER = LoggerFactory.getLogger(BitstampSimulationTradeFloor.class);

	private double tradeFee = 0.05d;
	
	public BitstampSimulationTradeFloor(ISignal last, ISignal bid, ISignal ask) {
		super(last, bid, ask);
	}
	
	@Override
	public double buyImpl(IWallet wallet, TradeAction action) {
		// We get the latest ask, assuming the ticker is updated by some other part of the app
		Sample currentAsk = askSignal.peek();
				
		// The amount of Btc we are going to get if we buy for volume USD
		double boughtBtc = action.volume * (1.0d-tradeFee);
		double soldUsd = action.volume * currentAsk.value;
		
		// We assume the trade is fulfilled instantly, for the price of the ask
		wallet.transact(action.timestamp, -soldUsd, boughtBtc);

		return boughtBtc;
	}

	@Override
	public double sellImpl(IWallet wallet, TradeAction action) {
		// We get the latest ask, assuming the ticker is updated by some other part of the app
		Sample currentBid = bidSignal.peek();
		
		// We assume the trade is fulfilled instantly, for the price of the ask
		double boughtUsd = currentBid.value * (action.volume * (1.0d-tradeFee)); // volume in bitcoins
		double soldBtc = action.volume;
		
		wallet.transact(action.timestamp, boughtUsd, -soldBtc);

		return boughtUsd;
	}

}
