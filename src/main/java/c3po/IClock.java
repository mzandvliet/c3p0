package c3po;

public interface IClock {
	public void addListener(IClockListener listener);
	public void removeListener(IClockListener listener);
	
	public void run();
}
