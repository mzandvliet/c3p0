package c3po;

public interface ITickable {
	public long getLastTick();
	public void tick(long tick);
}
