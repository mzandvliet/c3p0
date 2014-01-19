package c3po.structs;

/**
 * TradeIntention is a struct for all the parameters
 * that entail the desire to make a trade. It can be 
 * passed to the TradeFloor that will try to make it
 * happen.
 */
public class TradeIntention {
	final public TradeActionType action;
	final public long timestamp;
	final public double volume;

	public TradeIntention(TradeActionType action, long timestamp, double volume) {
		this.action = action;
		this.timestamp = timestamp;
		this.volume = volume;
	}

	public enum TradeActionType {
		BUY,
		SELL;
	}
	
	@Override
	public String toString() {
		return "TradeIntention [action=" + action + ", timestamp=" + timestamp + ", volume=" + volume + "]";
	}
}
