package c3po.structs;

public class OpenOrder {
	private final long id;
	private final long datetime;
	private final int type;
	private final double price;
	private final double amount;
	
	public final static int BUY = 0;
	public final static int SELL = 1;

	public OpenOrder(long id, long timestamp, int type, double price, double amount) {
		this.id = id;
		this.datetime = timestamp;
		this.type = type;
		this.price = price;
		this.amount = amount;
	}

	public long getId() {
		return id;
	}
	
	public long getDatetime() {
		return datetime;
	}

	public int getType() {
		return type;
	}
	
	public String getTypeDisplay() {
		switch(type) {
		case BUY: return "BUY";
		case SELL: return "SELL";
		default: return "UNKNOWN";
		}
	}


	public double getPrice() {
		return price;
	}

	public double getAmount() {
		return amount;
	}
	
	
	
	public String toString() {
		return String.format("OpenOrder %s #%d: %s for %s", getTypeDisplay(), id, amount, price);
	}
}
