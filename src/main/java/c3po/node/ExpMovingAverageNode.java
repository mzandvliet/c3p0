package c3po.node;

import c3po.AbstractTickable;
import c3po.ISignal;
import c3po.OutputSignal;
import c3po.Sample;
import c3po.utils.SignalMath;

public class ExpMovingAverageNode extends AbstractTickable implements INode {
	private ISignal input;
	private int kernelSize;
	private Sample lastSample;
	private OutputSignal output;
	
	public ExpMovingAverageNode(long timestep, long window, ISignal input) {
		super(timestep);
		this.input = input;
		this.kernelSize = (int) (window / timestep + 1);
		this.lastSample = Sample.none;
		this.output = new OutputSignal(this, "Exponential Moving Average");
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
		Sample newest = transform(lastSample, input.getSample(tick), kernelSize);
		lastSample = newest;
		output.setSample(newest);
	}

	private static Sample transform(Sample lastSample, Sample newest, int kernelSize) {
		return SignalMath.filterExpMovingAverage(lastSample, newest, kernelSize);
	}
}