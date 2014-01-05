package c3po.node;

import c3po.AbstractTickable;
import c3po.CircularArrayList;
import c3po.ISignal;
import c3po.OutputSignal;
import c3po.Sample;
import c3po.utils.SignalMath;

public class MovingAverageNode extends AbstractTickable implements INode {
	private ISignal input;
	private int kernelSize;
	private CircularArrayList<Sample> buffer;
	private OutputSignal output;
	
	public MovingAverageNode(long timestep, long window, ISignal input) {
		super(timestep);
		this.input = input;
		this.kernelSize = (int) Math.round(window / timestep);
		this.buffer = new CircularArrayList<Sample>(kernelSize);
		this.output = new OutputSignal(this, "Moving Average");
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
		Sample newest = input.getSample(tick);
		buffer.add(newest);
		output.setSample(SignalMath.basicMovingAverage(buffer));
	}
}