package c3po.structs;

/**
 * Actual done trade.
 * 
 * Should only be created by the Tradefloor as result
 * on a verified trade.
 */
public class TradeResult {
	private final long id;
	private final long timestamp;
	private final TradeActionType type;
	private final double price;
	private final double amount;
	
	public TradeResult(long id, long timestamp, TradeActionType type, double price, double amount) {
		this.id = id;
		this.timestamp = timestamp;
		this.type = type;
		this.price = price;
		this.amount = amount;
	}

	public long getId() {
		return id;
	}
	
	public long getTimestamp() {
		return timestamp;
	}

	public TradeActionType getType() {
		return type;
	}

	public double getPrice() {
		return price;
	}

	public double getAmount() {
		return amount;
	}
	
	
	public enum TradeActionType {
		BUY,
		SELL;
	}

	@Override
	public String toString() {
		return "TradeResult [id=" + id + ", timestamp=" + timestamp + ", type="	+ type + ", price=" + price + ", amount=" + amount + "]";
	}
}
