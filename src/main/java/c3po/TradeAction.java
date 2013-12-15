package c3po;

import c3po.utils.Time;

/* TODO:
 * 
 * - Reference or handle to bot who initiated the trade
 * - Separate concept of trade action from trade success/result
 */

public class TradeAction {
	final public TradeActionType action;
	final public long timestamp;
	final public double volume;

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
		return String.format("%s for %f %s on %s", action, volume, action == TradeActionType.BUY ? "Usd" : "Btc" , Time.format(timestamp));
	}
}
