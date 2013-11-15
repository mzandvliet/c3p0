package c3po;

import java.util.List;

public class ExpMovingAverageNode implements INode {
	private ISignal input;
	private int kernelSize;
	private CircularArrayList<Sample> buffer;
	private OutputSignal output;
	private long lastTick = -1;
	
	public ExpMovingAverageNode(ISignal input, int kernelSize) {
		this.input = input;
		this.kernelSize = kernelSize;
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
	public void tick(long tick) {
		if (tick > lastTick) {
			Sample newest = transform(buffer, input.getSample(tick), kernelSize);
			buffer.add(newest);
			output.setSample(newest);
		}
	}

	private Sample transform(List<Sample> lastSignals, Sample newest, int kernelSize) {
		return Indicators.filterExpMovingAverage(lastSignals, newest, kernelSize);
	}
}