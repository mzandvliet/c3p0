package c3po;

public abstract class AbstractTickable implements ITickable {
	private long lastTick = -1;
	
	@Override
	public long getLastTick() {
		return lastTick;
	}
	
	@Override
	public void tick(long tick) {
		if (tick > lastTick) {
			onNewTick(tick);
		}
		lastTick = tick;
	}
	
	public abstract void onNewTick(long tick);
}
