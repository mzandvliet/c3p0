package c3po;

public interface IBot extends ITickable, ITradeActionSource {
	public long getTimestep();
	public int hashCode();
}
