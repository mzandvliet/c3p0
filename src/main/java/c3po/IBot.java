package c3po;

public interface IBot extends ITickable, ITradeActionSource {
	public IWallet getWallet();
	public ITradeFloor getTradeFloor();
	public int hashCode();
}