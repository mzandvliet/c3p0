package c3po;

/* Todo: not done/happy with this */

public class MovingAverageSignal extends SignalBuffer {
	private int kernelSize;
	
	public MovingAverageSignal(ISignal source, int kernelSize) {
		super(source, kernelSize * 2); // Pad buffer length to push calculation error outside critical range
		this.kernelSize = kernelSize;
	}
	
	@Override
	public void tick(long tick) {
		if (tick > lastTick) {
			Sample newest = source.getSample(tick);
			newest = Indicators.filterExpMovingAverage(signals, newest, kernelSize);
			signals.enqueue(newest);
			lastTick = tick;
		}
	}
}
