package c3po;

public interface ITradeActionSource {
	public void addTradeListener(ITradeListener listener);
	public void removeListener(ITradeListener listener);
	public ITradeFloor getTradeFloor();
}
