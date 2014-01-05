package c3po.orderbook;

public class OrderPercentile {
	public double percentile;
	public double price;
	public double volume;
	
	public OrderPercentile(double percentile) {
		this.percentile = percentile;
		this.price = 0d;
		this.volume = 0d;
	}
	
	public OrderPercentile(double percentile, double price, double volume) {
		this.percentile = percentile;
		this.price = price;
		this.volume = volume;
	}
}