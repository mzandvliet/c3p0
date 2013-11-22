package c3po;

public interface IClock {
	public void addListener(ITickable listener);
	public void removeListener(ITickable listener);
	
	public void run();
}
