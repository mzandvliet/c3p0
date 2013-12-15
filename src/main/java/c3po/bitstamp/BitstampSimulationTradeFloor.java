package c3po.bitstamp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import c3po.*;
import c3po.wallet.IWallet;

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

	private double tradeFee = 0.33d / 100;
	
	public BitstampSimulationTradeFloor(ISignal last, ISignal bid, ISignal ask) {
		super(last, bid, ask, false);
	}
	
	@Override
	public double buyImpl(IWallet wallet, TradeAction action) {
		// NOTE: This assumes action.volume is in USD		
		
		// We get the latest ask, assuming the ticker is updated by some other part of the app
		Sample currentAsk = askSignal.peek();
				
		// The amount of BTC we are going to get if we buy for volume USD, with fees subtracted
		double boughtBtc = action.volume / currentAsk.value * (1.0d-tradeFee);
		
		// We assume the trade is fulfilled instantly, for the price of the ask
		wallet.transact(action.timestamp, -action.volume, boughtBtc);

		return boughtBtc;
	}

	@Override
	public double sellImpl(IWallet wallet, TradeAction action) {
		// NOTE: This assumes action.volume is in BTC	
		
		// We get the latest ask, assuming the ticker is updated by some other part of the app
		Sample currentBid = bidSignal.peek();
		
		// The amount of BTC we are going to get if we sell for volume BTC, with fees subtracted
		double boughtUsd = action.volume * currentBid.value * (1.0d-tradeFee);
		
		// We assume the trade is fulfilled instantly, for the price of the bid
		wallet.transact(action.timestamp, boughtUsd, -action.volume);

		return boughtUsd;
	}

	@Override
	public void updateWallet(IWallet wallet) {
		// TODO Auto-generated method stub
	}

	@Override
	public double peekBid() throws Exception {
		return bidSignal.peek().value;
	}
}
