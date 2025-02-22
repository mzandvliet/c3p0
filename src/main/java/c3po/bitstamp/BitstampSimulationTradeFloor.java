package c3po.bitstamp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import c3po.*;
import c3po.structs.TradeIntention;
import c3po.structs.TradeResult;
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

	private double tradeFee = 0.40d / 100;
	
	public BitstampSimulationTradeFloor(ISignal last, ISignal bid, ISignal ask) {
		super(last, bid, ask, false);
	}
	
	@Override
	public TradeResult buyImpl(long tick, IWallet wallet, TradeIntention action) {
		// NOTE: This assumes action.volume is in USD		
		
		// We get the latest ask, assuming the ticker is updated by some other part of the app
		Sample currentAsk = askSignal.getSample(tick);
				
		// The amount of BTC we are going to get if we buy for volume USD, with fees subtracted
		double boughtBtc = action.volume / currentAsk.value * (1.0d-tradeFee);
		
		// We assume the trade is fulfilled instantly, for the price of the ask
		wallet.modify(action.timestamp, -action.volume, boughtBtc);

		return new TradeResult(0, action.timestamp, TradeResult.TradeActionType.BUY, currentAsk.value, boughtBtc);
	}

	@Override
	public TradeResult sellImpl(long tick, IWallet wallet, TradeIntention action) {
		// NOTE: This assumes action.volume is in BTC	
		
		// We get the latest ask, assuming the ticker is updated by some other part of the app
		Sample currentBid = bidSignal.getSample(tick);
		
		// The amount of BTC we are going to get if we sell for volume BTC, with fees subtracted
		double boughtUsd = action.volume * currentBid.value * (1.0d-tradeFee);
		
		// We assume the trade is fulfilled instantly, for the price of the bid
		wallet.modify(action.timestamp, boughtUsd, -action.volume);

		return new TradeResult(0, action.timestamp, TradeResult.TradeActionType.SELL, currentBid.value, boughtUsd);
	}

	@Override
	public void updateWallet(IWallet wallet) {
		
	}
	
	
	@Override
	public ITradeFloor copy() {
		BitstampSimulationTradeFloor clone = new BitstampSimulationTradeFloor(this.tickerSignal, this.bidSignal, this.askSignal);
		for(ITradeListener listener : this.tradeListeners) {
			clone.addTradeListener(listener);
		}
		
		return clone;
	}
}
