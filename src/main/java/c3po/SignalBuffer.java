package c3po;

import java.util.*;

/*
 * Storing history of a signal in chronological order
 */
public class SignalBuffer extends AbstractList<Signal> implements ISignalBuffer, RandomAccess {
	private ISignalSource source;
	private CircularArrayList<Signal> signals;
	private long lastTick = -1;
	
	public SignalBuffer(ISignalSource source, int length) {
		this.source = source;
		this.signals = new CircularArrayList<Signal>(length);
	}
	
	@Override
	public int size() {
		return signals.size();
	}
	
	@Override
	public Signal get(int index) {
		return signals.get(index);
	}
	
	@Override
	public Signal get(long tick, int index) {
		update(tick);
		
		return signals.get(index);
	}

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
		
		return signals.get(signals.size()-1);
	}
	
	@Override
	public Signal getLatest(long tick) {
		update(tick);
		return signals.get(signals.size()-1);
	}
	
	@Override
	public Signal getOldest(long tick) {
		update(tick);
		return signals.get(0);
	}
	
	private void update(long tick) {
		if (tick > lastTick) {
			Signal newest = source.get(tick); 
			signals.enqueue(newest);
			lastTick = tick;
		}
	}
}
