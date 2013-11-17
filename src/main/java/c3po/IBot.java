package c3po;

public interface IBot extends ITickable {
	public long getTimestep();
	public ITradeFloor getTradeFloor();
}
