package c3po;

public class SubtractNode implements INode {
	private ISignal inputA;
	private ISignal inputB;
	private OutputSignal output;
	private long lastTick = -1;
	
	public SubtractNode(ISignal inputA, ISignal inputB) {
		this.inputA = inputA;
		this.inputB = inputB;
		this.output = new OutputSignal(this);
	}

	@Override
	public int getNumOutputs() {
		return 1;
	}

	@Override
	public ISignal getOutput(int i) {
		return output;
	}

	@Override
	public void tick(long tick) {
		if (tick > lastTick) {
			Sample newest = subtract(inputA.getSample(tick), inputB.getSample(tick));
			output.setSample(newest);
		}
	}

	private Sample subtract(Sample a, Sample b) {
		return new Sample(a.timestamp, a.value - b.value);
	}
}
