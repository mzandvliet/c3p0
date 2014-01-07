package c3po.composite;

import c3po.Sample;
import c3po.simulation.ITradeAdviceSignal;

/**
 * This class makes it possible for bots to wrap the tradeAdvice and the
 * config for that tradeAdvice together. This way we can give the composite
 * bot for instance a list of WeightedTradeAdviceSignals. And we can train
 * the bot to adjust these signals for optimal performance.
 */
public class WeightedTradeAdviceSignal {
	private final double buyAdviceMultiplier;
	private final double sellAdviceMultiplier;
	private final ITradeAdviceSignal tradeAdviceSignal;
	
	public WeightedTradeAdviceSignal(ITradeAdviceSignal tradeAdviceSignal, double buyAdviceMultiplier, double sellAdviceMultiplier) {
		this.buyAdviceMultiplier = buyAdviceMultiplier;
		this.sellAdviceMultiplier = sellAdviceMultiplier;
		this.tradeAdviceSignal = tradeAdviceSignal;
	}
	
	public double getBuyAdviceMultiplier() {
		return buyAdviceMultiplier;
	}
	
	public double getSellAdviceMultiplier() {
		return sellAdviceMultiplier;
	}
	
	public ITradeAdviceSignal getTradeAdviceSignal() {
		return tradeAdviceSignal;
	}
	
	public Sample getWeightedAdviceSample(long tick) {
		Sample sample = tradeAdviceSignal.getOutputTradeAdvice().getSample(tick);
		
		double weightedAdvice = 0;
		if(sample.value > 0) {
			weightedAdvice = sample.value * buyAdviceMultiplier;
		} else if(sample.value < 0) {
			weightedAdvice = sample.value * sellAdviceMultiplier;
		}
		
		return new Sample(sample.timestamp, weightedAdvice);
	}
}
