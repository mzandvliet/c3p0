package c3po;

import java.util.Date;

public class TradeAction {
	final public double volume;
	final public TradeActionType action; // enum would be nicer
	final public long timestamp;

	public TradeAction(TradeActionType action, long timestamp, double volume) {
		this.action = action;
		this.timestamp = timestamp;
		this.volume = volume;
	}

	public enum TradeActionType {
		BUY,
		SELL;
	}
	
	public String toString() {
		return String.format("%s for %f %s on %s", action, volume, action == TradeActionType.BUY ? "Usd" : "Btc" , new Date(timestamp));
	}
}
