package c3po.orderbook;

public interface IOrderBookSource {
	public OrderBookSample getSample(long tick);
}
