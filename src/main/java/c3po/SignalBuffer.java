package c3po;

import java.util.*;

/*
 * Storing history of a signal in chronological order
 */
public class SignalBuffer extends AbstractList<Signal> implements ISignalBuffer, RandomAccess {
	private ISignalSource source;
	private ISignalTransformer transformer;
	private CircularArrayList<Signal> signals;
	private long lastTick = -1;
	
	public SignalBuffer(ISignalSource source, int length) {
		this.source = source;
		this.signals = new CircularArrayList<Signal>(length);
	}
	
	public SignalBuffer(ISignalSource source, int length, ISignalTransformer transformer) {
		this(source, length);
		this.transformer = transformer;
	}
	
	@Override
	public int size() {
		return signals.size();
	}
	
	@Override
	public Signal get(int index) {
		return Signal.copy(signals.get(index));
	}
	
	@Override
	public Signal getLatest(long tick) {
		update(tick);
		
		return Signal.copy(signals.get(signals.size()-1));
	}
	
	@Override
	public Signal getOldest(long tick) {
		update(tick);
		return Signal.copy(signals.get(0));
	}
	
	@Override
	public Signal get(long tick, int index) {
		update(tick);
		
		return Signal.copy(signals.get(index));
	}

	// Todo: inject interpolation strategy
	@Override
	public Signal getInterpolated(long tick, long timestamp) {
		update(tick);
		
		Signal oldest = signals.get(0);
		if (timestamp <= oldest.timestamp)
			return oldest;

		for (int i = 0; i < signals.size(); i++) {
			Signal current = signals.get(i);
			if (current.timestamp >= timestamp) {
				return Indicators.lerp(current, signals.get(i-1), timestamp);
			}
		}
		
		return Signal.copy(signals.get(signals.size()-1));
	}
	
	private void update(long tick) {
		if (tick > lastTick) {
			Signal newest = source.getLatest(tick);
			if (transformer != null) {
				transformer.transform(signals, newest);
			}				
			signals.enqueue(newest);
			lastTick = tick;
		}
	}
}
