package c3po.structs;

public class OpenOrder {
	private final long id;
	private final long datetime;
	private final String type;
	private final double price;
	private final double amount;

	public OpenOrder(long id, long datetime, String type, double price, double amount) {
		this.id = id;
		this.datetime = datetime;
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

	public String getType() {
		return type;
	}

	public double getPrice() {
		return price;
	}

	public double getAmount() {
		return amount;
	}
}
