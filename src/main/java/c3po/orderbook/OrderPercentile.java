package c3po.orderbook;

public class OrderPercentile {
	public final double percentile;
	public final double price;
	public final double volume;
	
	public OrderPercentile(double percentile, double price, double volume) {
		this.percentile = percentile;
		this.price = price;
		this.volume = volume;
	}
}
