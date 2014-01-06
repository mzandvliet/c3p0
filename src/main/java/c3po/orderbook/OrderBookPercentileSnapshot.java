package c3po.orderbook;

import java.util.List;

public class OrderBookPercentileSnapshot {
	public final long timestamp;
	
	public final double highestBid;
	public final double lowestAsk;
	
	public final List<OrderPercentile> bids;
	public final List<OrderPercentile> asks;
	
	public OrderBookPercentileSnapshot(
			long timestamp,
			double highestBid,
			double lowestAsk, List<OrderPercentile> bids,
			List<OrderPercentile> asks) {
		this.timestamp = timestamp;
		this.highestBid = highestBid;
		this.lowestAsk = lowestAsk;
		this.bids = bids;
		this.asks = asks;
	}
}
