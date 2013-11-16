package c3po;

/*
 * Used to encapsulate a single output value for a Node
 * 
 * Ticks its parent when queried for new value. Parent should then supply it with latest Sample.
 */
public class OutputSignal implements ISignal {
	private ITickable ownerNode;
	private Sample latestSample;
	
	public OutputSignal(ITickable node) {
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
		tick(tick);
		return Sample.copy(latestSample);
	}
	
	@Override
	public void tick(long tick) {
		ownerNode.tick(tick); // Tick the node that owns this output, which then updates this output's value
	}
	
	public String toString() {
		return String.format("%s from %s", latestSample, ownerNode);
	}
}