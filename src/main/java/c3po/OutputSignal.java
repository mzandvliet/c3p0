package c3po;

/*
 * Used to encapsulate a single output value for a Node
 * 
 * Ticks its parent when queried for new value. Parent should then supply it with latest Sample.
 */
public class OutputSignal implements ISignal {
	private INode ownerNode;
	private Sample latestSample = Sample.none;
	
	public OutputSignal(INode node) {
		this.ownerNode = node;
	}
	
	public void setSample(Sample sample) {
		latestSample = sample;
	}
	
	@Override
	public Sample peek() {
		return latestSample;
	}
	
	@Override
	public Sample getSample(long tick) {
		ownerNode.tick(tick);
		return Sample.copy(latestSample);
	}
	
	public String toString() {
		return String.format("%s from %s", latestSample, ownerNode);
	}
}