package c3po.orderbook;

import java.util.List;

public class OrderBookPercentileSnapshot {
	public final long timestamp;
	
	public final List<OrderPercentile> bids;
	public final List<OrderPercentile> asks;

	public OrderBookPercentileSnapshot(long timestamp, List<OrderPercentile> bids, List<OrderPercentile> asks) {
		
		this.timestamp = timestamp;
		this.bids = bids;
		this.asks = asks;
	}
}
