package c3po.orderbook;

import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import c3po.*;
import c3po.node.INode;

public class OrderBookPricePercentileTransformer extends OrderBookPercentileTransformer implements INode {
	private static final Logger LOGGER = LoggerFactory.getLogger(OrderBookPricePercentileTransformer.class);
	
	private final IOrderBookSource source;
	
	public OrderBookPricePercentileTransformer(long timestep, long interpolationTime, double[] percentiles, IOrderBookSource source) {
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
//		List<Order> asks = sample.asks;
		
		removeDeviantOrders(bids, 0.95d);
//		removeDeviantOrders(asks, 0.95d);
		
		double lowestBid = bids.get(bids.size()-1).price;
		double highestBid = bids.get(0).price;
//		double lowestAsk = asks.get(0).price;
//		double highestAsk = asks.get(asks.size()-1).price;
		
		// Calculate percentiles and format them to signals
		
		HashMap<Double, Double> bidPercentiles = calculateBidPercentiles(bids, lowestBid, highestBid, percentiles);
		//HashMap<Double, Double> askPercentiles = calculateBidPercentiles(asks, lowestAsk, highestAsk, percentiles);
		
		ServerSnapshot snapshot = new ServerSnapshot(sample.timestamp, 2 + 2 * percentiles.length);
		
		snapshot.set(0, new Sample(sample.timestamp, 0d));
		//entry.set(1, new Sample(sample.timestamp, 0d));
		
		setSnapshotValues(snapshot, bidPercentiles, "BID", 2);
		//setSnapshotValues(entry, askPercentiles, "ASK", 2 + percentiles.length);
		
		buffer.add(snapshot);
	}
	
	/**
	 * Removes orders who's prices deviate more than maxDeviation from the average price
	 * @param orders
	 * @param maxDeviation Maximum deviation from average. Range: 0.0 < n < 1.0
	 */
	private static void removeDeviantOrders(List<Order> orders, double maxDeviation) {
		double averagePrice = 0;
		
		for (int i = 0; i < orders.size(); i++) {
			Order order = orders.get(i);
			averagePrice += order.price;
		}
		
		averagePrice /= (double)orders.size();
		double minPrice = averagePrice - averagePrice * maxDeviation;
		double maxPrice = averagePrice + averagePrice * maxDeviation;
		
		for (int i = 0; i < orders.size(); i++) {
			Order order = orders.get(i);
			if (order.price < minPrice || order.price > maxPrice) {
				orders.remove(i);
				i--;
			}
		}
	}
	
	/*
	 *  Bids are ordered ASCENDING price
	 *  Asks are ordered by DESCENDING price
	 *  
	 *  Orders with prices closest to LAST are first in the arrays.
	 *  
	 *  Example of non-linearly distributed percentiles: [99, 98.5, 98, 97, 96, 95, 90, 85, 75]
	 *  
	 *  High percentiles should map to orders close to LAST, low percentiles to orders far away from LAST.
	 *  
	 *  TODO: 
	 *  - Maybe we should just enforce linear percentiles definitions at this point. 
	 *  	- Non-linear definitions can be interpolated to linear. And using these non-linear lists makes calculations and apis very messy.
	 */
	
	private static HashMap<Double, Double> calculateBidPercentiles(final List<Order> orders, final double minPrice, final double maxPrice, double[] percentiles) {
		final HashMap<Double, Double> percentileValues = new HashMap<Double, Double>(); // TODO: this is inefficient. Cache it?
		
		int percentileIndex = 0;
		double aggregatedVolume = 0d;
		
		for (int i = 0; i < orders.size(); i++) {
			Order order = orders.get(i);
			
			double percentile = percentiles[percentileIndex];
			double percentilePriceThreshold = maxPrice * (percentile / 100d);
			
			aggregatedVolume += order.volume;
			
			if (order.price < percentilePriceThreshold) {
				// Normalize percentile values in case we have a non-linear distribution of percentile points
//				double lastPercentile = percentileIndex > 0 ? percentiles[percentileIndex-1] : 100d;
//				double normalizedAggregatedVolume = aggregatedVolume / (percentile - lastPercentile);
				
				percentileValues.put(new Double(percentile), new Double(aggregatedVolume));
				LOGGER.debug(percentile + ", " + aggregatedVolume);
				
				aggregatedVolume = 0d;
				percentileIndex++;
			}
			
			// TODO: This breaks at very low percentiles because the orders in that range have likely been filtered out earlier
			
			if (percentileIndex >= percentiles.length)
				break;
		}
		
		return percentileValues;
	}
	
	private static void setSnapshotValues(ServerSnapshot entry, final HashMap<Double, Double> percentiles, final String orderType, final int startIndex) {
		int index = startIndex;
		for (Entry<Double, Double> percentile : percentiles.entrySet()) {
			entry.set(index, new Sample(entry.timestamp, percentile.getValue().doubleValue()));
			index++;
		}
	}
}
