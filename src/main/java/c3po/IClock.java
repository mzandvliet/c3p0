package c3po;

public interface IClock {
	public void addListener(ITickable tickable);
	public void removeListener(ITickable tickable);
	
	public void run();
}
