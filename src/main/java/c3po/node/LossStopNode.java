package c3po.node;

import c3po.AbstractTickable;
import c3po.ISignal;
import c3po.ITradeListener;
import c3po.OutputSignal;
import c3po.Sample;
import c3po.TradeAction;
import c3po.TradeAction.TradeActionType;
import c3po.simulation.ITradeAdviceSignal;

/**
 * This node gives sell advice when the price drops
 * after having bought. This class only gives sell advice.
 */
public class LossStopNode extends AbstractTickable implements INode, ITradeAdviceSignal, ITradeListener {
	/**
	 * Signal between 0 (neutral) and -1 (must sell)
	 */
	private OutputSignal tradeAdvice;
	
	/**
	 * Signal that the advice is based on
	 */
	private final ISignal priceSignal;
	
	/**
	 * Sample on the moment when we last opened position
	 */
	private Sample lastBuySample;
	
	/**
	 * We use this to get the price on the buy moment. Not sure if it's the best solution
	 * or we should peek the signal or something.
	 */
	private Sample lastPriceSample;
	
	/**
	 * Sample with the highest value after the last buy.
	 */
	private Sample highestPriceSinceBuySample;
	
	/**
	 * Config that helps deciding the tradeAdvice
	 */
	private final LossStopNodeConfig config;

	public LossStopNode(long timestep, ISignal priceSignal, LossStopNodeConfig config) {
		super(timestep);
		this.priceSignal = priceSignal;
		this.config = config;
		this.tradeAdvice = new OutputSignal(this, "LossStop TradeAdvice");
	}
	
	@Override
	public int getNumOutputs() {
		return 1;
	}

	@Override
	public ISignal getOutput(int i) {
		return tradeAdvice;
	}

	@Override
	public void update(long tick) {
		lastPriceSample = priceSignal.getSample(tick);
		
		// Keep track of the highest price since buy
		if(lastPriceSample.value > highestPriceSinceBuySample.value)
			highestPriceSinceBuySample = lastPriceSample;
		
		if(lastBuySample != null) {
			// Calculate the new trade advice and set that as output signal
			tradeAdvice.setSample(new Sample(tick, calculateTradeAdvice(lastBuySample.value, highestPriceSinceBuySample.value, config.getIgnoreLossPercentage(), config.getMaxLossPercentage())));
		}
	}
	
	/**
	 * Helper method to calculate the trade advice based on config and last prices
	 * 
	 * @param lastBuyPrice
	 * @param currentPrice
	 * @param ignoreLossPercentage
	 * @param maxLossPercentage
	 * @return Advice between 0 (neutral) and -1 (sell)
	 */
	public static double calculateTradeAdvice(double lastBuyPrice, double currentPrice, double ignoreLossPercentage, double maxLossPercentage) {
		// Difference between last buy and current, negated so the higher, the more loss
		double negDiff = (currentPrice-lastBuyPrice)/lastBuyPrice * 100 * -1;
	
		if(negDiff > ignoreLossPercentage) {
			return Math.min(1, (negDiff - ignoreLossPercentage) / (maxLossPercentage - ignoreLossPercentage)) * -1;
		} 
		// Not enough loss yet to care
		else {
			return 0;
		}
	}

	@Override
	public void onTrade(TradeAction action) {
		if(action.action == TradeActionType.BUY) {
			this.lastBuySample = new Sample(action.timestamp, lastPriceSample.value);
			this.highestPriceSinceBuySample = lastBuySample;
		}
	}

	@Override
	public ISignal getOutputTradeAdvice() {
		return tradeAdvice;
	}

	public Sample getLastBuySample() {
		return lastBuySample;
	}

	public void setLastBuySample(Sample lastBuySample) {
		this.lastBuySample = lastBuySample;
	}

	public double getIgnoreLossPercentage() {
		return config.getIgnoreLossPercentage();
	}

	public double getMaxLossPercentage() {
		return config.getMaxLossPercentage();
	}
}
