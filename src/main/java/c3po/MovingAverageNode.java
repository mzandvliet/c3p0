package c3po;

import java.util.List;

public class MovingAverageNode extends AbstractTickable implements INode {
	private ISignal input;
	private int kernelSize;
	private CircularArrayList<Sample> buffer;
	private OutputSignal output;
	
	public MovingAverageNode(long timestep, long window, ISignal input) {
		super(timestep);
		this.input = input;
		this.kernelSize = (int) (window / timestep + 1);
		this.buffer = new CircularArrayList<Sample>(kernelSize * 2);
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
	public void onNewTick(long tick) {
		buffer.add(input.getSample(tick));
		output.setSample(SignalMath.basicMovingAverage(buffer, kernelSize));
	}
}