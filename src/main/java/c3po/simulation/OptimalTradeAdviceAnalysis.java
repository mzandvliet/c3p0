package c3po.simulation;

import java.util.List;
import java.util.LinkedList;

import c3po.AbstractTickable;
import c3po.ISignal;
import c3po.OutputSignal;
import c3po.Sample;
import c3po.node.INode;

/**
 * The goal of this class is to calculate over a set of data the most ideal trade advice.
 * @author Joost
 *
 */
public class OptimalTradeAdviceAnalysis extends AbstractTickable implements INode, ITradeAdviceSignal {

	private long tradePeriod;
	private LinkedList<Sample> priceHistory = new LinkedList<Sample>();
	private ISignal priceSignal;
	private OutputSignal adviceSignal;
	private long lastFetchedTimestamp;

	public OptimalTradeAdviceAnalysis(long timestep, ISignal priceSignal, long tradePeriod) {
		super(timestep);
		this.tradePeriod = tradePeriod;
		this.priceSignal = priceSignal;
		this.adviceSignal = new OutputSignal(this, "tradeAdvice");
	}

	@Override
	public int getNumOutputs() {
		return 1;
	}

	@Override
	public ISignal getOutput(int i) {
		return this.adviceSignal;
	}

	@Override
	public void onNewTick(long tick) {
		long lastNeededTimestamp = tick + tradePeriod;
		
		if(lastFetchedTimestamp <= 0) {
			lastFetchedTimestamp = tick - this.getTimestep();
		}
		
		// Update the price history
		removeOldSamples(priceHistory, tick);
		
		// Fetch missing data
		while(lastNeededTimestamp > lastFetchedTimestamp) {
			Sample sample = priceSignal.getSample(lastFetchedTimestamp + this.getTimestep());
			priceHistory.add(sample);
			lastFetchedTimestamp = sample.timestamp;
		} 
		
		
		// Here I need to calculate the new advice for this period
		this.adviceSignal.setSample(new Sample(tick, calculateTradeAdvice(priceHistory)));
	}
	
	/**
	 * Remove all data older then maxTime
	 * 
	 * @param samples
	 * @param maxTime
	 * @return
	 */
	public static LinkedList<Sample> removeOldSamples(LinkedList<Sample> samples, long maxTime) {
		// List to store the stuff in we are going to remove 
		// We dont do this in the forloop itself since then we get a ConcurrentModificationException
		List<Sample> toRemove = new LinkedList<Sample>();
		for(Sample sample : samples) {
			if(sample.timestamp < maxTime) {
				toRemove.add(sample);
			} else {
				// Timestamps are sequential so we can break after a miss
				break;
			}
		}
		
		// Remove the samples that were scheduled for removal
		for(Sample sample : toRemove) {
			samples.remove(sample);
		}
		
		return samples;
	}
	
	public static double calculateTradeAdvice(LinkedList<Sample> priceHistory) {
		Sample first = priceHistory.getFirst();
		Sample last = priceHistory.getLast();
		
		// Procentual difference 
		// Modify so that a procentual increase is an increase of the diff by 1
		double diff = (last.value - first.value) / first.value * 100;
		
		return Math.min(1, Math.max(-1, diff));
	}

	@Override
	public ISignal getOutputTradeAdvice() {
		return this.adviceSignal;
	}
}
