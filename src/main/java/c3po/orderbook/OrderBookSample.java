package c3po.orderbook;

import java.util.List;

public class OrderBookSample {
	public final long timestamp;
	public final List<Order> bids;
	public final List<Order> asks;
	
	public OrderBookSample(long timestamp, List<Order> latestBids,
			List<Order> latestAsks) {
		
		this.timestamp = timestamp;
		this.bids = latestBids;
		this.asks = latestAsks;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((asks == null) ? 0 : asks.hashCode());
		result = prime * result
				+ ((bids == null) ? 0 : bids.hashCode());
		result = prime * result + (int) (timestamp ^ (timestamp >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		OrderBookSample other = (OrderBookSample) obj;
		if (asks == null) {
			if (other.asks != null)
				return false;
		} else if (!asks.equals(other.asks))
			return false;
		if (bids == null) {
			if (other.bids != null)
				return false;
		} else if (!bids.equals(other.bids))
			return false;
		if (timestamp != other.timestamp)
			return false;
		return true;
	}
}