package c3po;

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