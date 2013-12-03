package c3po;

/**
 * This class abstracts away the concept of timestep and
 * only ripples through the ticks that occur after the
 * length of a timestep.
 */
public abstract class AbstractTickable implements ITickable {
	private final long timestep;
	private long lastTick = -1;
	
	public AbstractTickable(long timestep) {
		this.timestep = timestep;
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
			onNewTick(tick);
		
			lastTick = tick;
		}
	}
	
	public void reset() {
		lastTick = -1;
	}
	
	public abstract void onNewTick(long tick);
}
