package c3po;

/*
 * Used to encapsulate a single part of a multi-part data stream as a signal.
 * 
 * Ticks its parent when queried for new value. Parent should then supply it with latest Sample.
 */

public class SurrogateSignal implements ISignal {
	private ITickable parent;
	private Sample latestSample;
	
	public SurrogateSignal(ITickable parent) {
		this.parent = parent;
	}
	
	public void setSample(Sample sample) {
		latestSample = sample;
	}
	
	public Sample getSample(long tick) {
		tick(tick);
		return Sample.copy(latestSample);
	}
	
	public void tick(long tick) {
		parent.tick(tick);
	}
}