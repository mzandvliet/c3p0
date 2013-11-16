package c3po;

public interface IClock {
	public void addListener(IBot bot);
	public void removeListener(IBot bot);
	
	public void run();
}
