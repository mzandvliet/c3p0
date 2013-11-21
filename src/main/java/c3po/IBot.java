package c3po;

public interface IBot extends IClockListener, ITradeActionSource {
	public IWallet getWallet();
	public ITradeFloor getTradeFloor();
	public int hashCode();
}