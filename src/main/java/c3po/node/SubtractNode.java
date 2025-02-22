package c3po.node;

import c3po.AbstractTickable;
import c3po.ISignal;
import c3po.OutputSignal;
import c3po.Sample;

public class SubtractNode extends AbstractTickable implements INode {
	private ISignal inputA;
	private ISignal inputB;
	private OutputSignal output;
	
	public SubtractNode(long timestep, ISignal inputA, ISignal inputB) {
		super(timestep);
		this.inputA = inputA;
		this.inputB = inputB;
		this.output = new OutputSignal(this, "Substration");
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
	public void update(long tick) {
		Sample newest = subtract(inputA.getSample(tick), inputB.getSample(tick));
		output.setSample(newest);
	}

	private Sample subtract(Sample a, Sample b) {
		return new Sample(a.timestamp, a.value - b.value);
	}
	
	public String toString() {
		return String.format("SubstractNode [%s - %s]", inputA, inputB);
	}
}
