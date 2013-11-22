package c3po;

public interface ITickable {
	public long getTimestep();
	public long getLastTick();
	public void tick(long tick);
}
