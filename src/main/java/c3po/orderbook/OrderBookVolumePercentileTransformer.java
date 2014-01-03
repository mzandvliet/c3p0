package c3po.orderbook;

import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import c3po.*;
import c3po.node.INode;

public class OrderBookVolumePercentileTransformer extends OrderBookPercentileTransformer implements INode {
	private static final Logger LOGGER = LoggerFactory.getLogger(OrderBookVolumePercentileTransformer.class);
	
	private final IOrderBookSource source;
	
	public OrderBookVolumePercentileTransformer(long timestep, long interpolationTime, double[] percentiles, IOrderBookSource source) {
		super(timestep, interpolationTime, percentiles);
		this.source = source;
	}

	@Override
	protected void pollServer(long clientTimestamp) {
		OrderBookSample sample = source.getSample(clientTimestamp);
		transform(sample);
	}

	public void transform(OrderBookSample sample) {
		// Early out if we already have the latest snapshot from the server
		long lastServerTimestamp = buffer.size() != 0 ? buffer.get(buffer.size()-1).timestamp : 0;
		if (sample.timestamp == lastServerTimestamp) {
			LOGGER.debug("Skipping duplicate server entry for timestamp " + sample.timestamp);
			return;
		}
		
		List<Order> bids = sample.bids;
		List<Order> asks = sample.asks;
		
		double totalBidVolume = calculateTotalOrderVolume(bids);
		double totalAskVolume = calculateTotalOrderVolume(asks);
		double totalVolume = totalBidVolume + totalAskVolume;
		
		// Print some general market stats, because hey they're cool! (TODO: totally do this somewhere else)
		
		double avgBidVolume = totalBidVolume / (double)bids.size();
		double avgAskVolume = totalAskVolume / (double)asks.size();
		LOGGER.debug(String.format("Market Stats:\nbids: [num %s, sum $%,.2f, average $%,.2f]\nasks: [num %s, sum $%,.2f, average $%,.2f]\ntotal bid/ask: %,.2f, average bid/ask: %,.2f",
				bids.size(),
				totalBidVolume,
				avgBidVolume,
				asks.size(),
				totalAskVolume,
				avgAskVolume,
				totalBidVolume / totalAskVolume,
				avgBidVolume / avgAskVolume
		));
		
		// Calculate percentiles and format them to signals
		
		HashMap<Double, Double> bidPercentiles = calculatePercentiles(bids, totalVolume, percentiles); // instead of totalBidVolume
		HashMap<Double, Double> askPercentiles = calculatePercentiles(asks, totalVolume, percentiles); // instead totalAskVolume
		
		ServerSnapshot entry = new ServerSnapshot(sample.timestamp, 2 + 2 * percentiles.length);
		
		entry.set(0, new Sample(sample.timestamp, totalBidVolume));
		entry.set(1, new Sample(sample.timestamp, totalAskVolume));
		
		setServerEntryValues(entry, bidPercentiles, "BID", 2);
		setServerEntryValues(entry, askPercentiles, "ASK", 2 + percentiles.length);
		
		buffer.add(entry);
	}

	private static HashMap<Double, Double> calculatePercentiles(final List<Order> orders, final double totalVolume, double[] percentiles) {
		final HashMap<Double, Double> percentileValues = new HashMap<Double, Double>(); // TODO: this is inefficient. Cache it?
		
		int percentileIndex = 0;
		double volumeParsed = 0d;
		
		for (Order order : orders) {			
			double currentPercentile = percentiles[percentileIndex];
			double percentileVolumeThreshold = totalVolume * ((100 - currentPercentile) / 100d);
			
			if (volumeParsed + order.volume > percentileVolumeThreshold) {
				double percentilePrice = order.price;
				
				percentileValues.put(new Double(currentPercentile), new Double(percentilePrice));
				LOGGER.debug(currentPercentile + ", " + percentilePrice);
				percentileIndex++;
			}
			
			volumeParsed += order.volume;
			
			if (percentileIndex >= percentiles.length)
				break;
		}
		
		return percentileValues;
	}
	
	private static void setServerEntryValues(ServerSnapshot entry, final HashMap<Double, Double> percentiles, final String orderType, final int startIndex) {
		int index = startIndex;
		for (Entry<Double, Double> percentile : percentiles.entrySet()) {
			entry.set(index, new Sample(entry.timestamp, percentile.getValue().doubleValue()));
			index++;
		}
	}

	private double calculateTotalOrderVolume(List<Order> orders) {
		double totalVolume = 0;
		for (Order order : orders) {
			totalVolume += order.volume;
		}
		return totalVolume;
	}
}
