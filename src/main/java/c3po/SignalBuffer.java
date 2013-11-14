package c3po;

import java.util.*;

/*
 * Storing history of a signal in chronological order
 */
public class SignalBuffer extends AbstractList<Sample> implements ISignalBuffer, RandomAccess {
	private ISignal source;
	private CircularArrayList<Sample> signals;
	private long lastTick = -1;
	
	public SignalBuffer(ISignal source, int length) {
		this.source = source;
		this.signals = new CircularArrayList<Sample>(length);
	}
	
	@Override
	public ISignal getSignal() {
		return source;
	}

	@Override
	public int size() {
		return signals.size();
	}
	
	@Override
	public Sample get(int index) {
		return Sample.copy(signals.get(index));
	}
	
	@Override
	public Sample getSample(long tick) {
		tick(tick);
		return Sample.copy(source.getSample(tick));
	}

	@Override
	public Sample getSample(long tick, int index) {
		tick(tick);
		
		return Sample.copy(signals.get(index));
	}

	// Todo: inject interpolation strategy
	@Override
	public Sample getInterpolatedSample(long tick, long timestamp) {
		tick(tick);
		
		// Return oldest
		Sample oldest = signals.get(0);
		if (timestamp <= oldest.timestamp)
			return Sample.copy(oldest);

		// Return interpolated
		for (int i = 0; i < signals.size(); i++) {
			Sample current = signals.get(i);
			if (current.timestamp >= timestamp) {
				return Indicators.lerp(current, signals.get(i-1), timestamp);
			}
		}
		
		// Return newest
		return Sample.copy(signals.get(signals.size()-1));
	}
	
	public void tick(long tick) {
		if (tick > lastTick) {
			signals.enqueue(source.getSample(tick));
			lastTick = tick;
		}
	}
}
