package c3po;

import java.util.*;

import c3po.utils.SignalMath;

/*
 * Stores history of a signal in chronological order.
 * 
 * When buffer length reaches max, oldest value is automatically dropped.
 */
public class SignalBuffer extends AbstractList<Sample> implements ISignalBuffer, RandomAccess {
	protected final ISignal source;
	protected final CircularArrayList<Sample> signals;
	protected final long timestep;
	protected long lastTick = -1;
	
	public SignalBuffer(long timestep, ISignal source, int length) {
		this.timestep = timestep;
		this.source = source;
		this.signals = new CircularArrayList<Sample>(length);
	}
	
	@Override
	public ISignal getInput() {
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
				return SignalMath.interpolate(current, signals.get(i-1), timestamp);
			}
		}
		
		// Return newest
		return Sample.copy(signals.get(signals.size()-1));
	}
	
	@Override
	public long getTimestep() {
		return timestep;
	}

	@Override
	public long getLastTick() {
		return lastTick;
	}
	
	@Override
	public void tick(long tick) {
		if (tick >= lastTick + timestep) {
			signals.add(source.getSample(tick));
		
			lastTick = tick;
		}
	}

	@Override
	public String getName() {
		return "Buffer of " + this.source.getName();
	}
}
