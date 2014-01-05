package c3po.orderbook;

import java.util.List;

public class OrderBookPercentileSnapshot {
	public final long timestamp;
	
	public final double lowestBid;
	public final double highestBid;
	public final double lowestAsk;
	public final double highestAsk;
	
	public final List<OrderPercentile> bids;
	public final List<OrderPercentile> asks;

	public OrderBookPercentileSnapshot(long timestamp, double lowestBid,
			double highestBid, double lowestAsk, double highestAsk,
			List<OrderPercentile> bids, List<OrderPercentile> asks) {
		
		this.timestamp = timestamp;
		this.lowestBid = lowestBid;
		this.highestBid = highestBid;
		this.lowestAsk = lowestAsk;
		this.highestAsk = highestAsk;
		this.bids = bids;
		this.asks = asks;
	}
}
