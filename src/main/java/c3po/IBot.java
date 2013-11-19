package c3po;

public interface IBot extends ITickable, ITradeActionSource {
	public long getTimestep();
	public IWallet getWallet();
	public ITradeFloor getTradeFloor();
	public int hashCode();
}