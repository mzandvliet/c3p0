package c3po.orderbook;

import java.util.ArrayList;
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
		List<Order> asks = sample.asks;
		
		removeStatisticallyDeviantOrders(bids, 0.95d);
		removeStatisticallyDeviantOrders(asks, 0.95d);
		
		double lowestBid = bids.get(bids.size()-1).price;
		double highestBid = bids.get(0).price;
		double lowestAsk = asks.get(0).price;
		double highestAsk = asks.get(asks.size()-1).price;
		
		// Calculate percentiles and format them to signals
		
		List<OrderPercentile> bidPercentiles = calculateBidPercentiles(bids, lowestBid, highestBid, percentiles);
		List<OrderPercentile> askPercentiles = calculateAskPercentiles(asks, lowestAsk, highestAsk, percentiles);
		
		ServerSnapshot snapshot = new ServerSnapshot(sample.timestamp, 2 + 2 * percentiles.length);
		
		snapshot.set(0, new Sample(sample.timestamp, 0d));
		snapshot.set(1, new Sample(sample.timestamp, 0d));
		
		setSnapshotValues(snapshot, bidPercentiles, "BID", 2);
		setSnapshotValues(snapshot, askPercentiles, "ASK", 2 + percentiles.length);
		
		buffer.add(snapshot);
	}
	
	/**
	 * Removes orders who's prices deviate more than maxDeviation from the average price
	 * @param orders
	 * @param maxDeviation Maximum deviation from average. Range: 0.0 < n < 1.0
	 */
	private static void removeStatisticallyDeviantOrders(List<Order> orders, double maxDeviation) {
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
	 *  Bids are ordered by ASCENDING price
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
	
	private static List<OrderPercentile> calculateBidPercentiles(final List<Order> orders, final double minPrice, final double maxPrice, double[] percentiles) {
		final List<OrderPercentile> percentileValues = new ArrayList<OrderPercentile>();
		
		for (int i = 0; i < percentiles.length; i++) {
			percentileValues.add(new OrderPercentile(percentiles[i]));
		}
		
		int percentileIndex = 0;
		double aggregatedVolume = 0d;
		
		for (int i = 0; i < orders.size(); i++) {
			Order order = orders.get(i);
			
			OrderPercentile orderPercentile = percentileValues.get(percentileIndex);
			double percentilePriceThreshold = maxPrice * (orderPercentile.percentile / 100d);
			
			aggregatedVolume += order.volume;
			
			if (order.price < percentilePriceThreshold) {
				// Normalize percentile values in case we have a non-linear distribution of percentile points
//				double lastPercentile = percentileIndex > 0 ? percentiles[percentileIndex-1] : 100d;
//				double normalizedAggregatedVolume = aggregatedVolume / (percentile - lastPercentile);
				
				orderPercentile.price = percentilePriceThreshold;
				orderPercentile.volume = aggregatedVolume;
				LOGGER.debug(orderPercentile.percentile + ", " + aggregatedVolume);
				
				aggregatedVolume = 0d;
				percentileIndex++;
			}

			if (percentileIndex >= percentiles.length)
				break;
		}
		
		return percentileValues;
	}
	
	private static List<OrderPercentile> calculateAskPercentiles(final List<Order> orders, final double minPrice, final double maxPrice, double[] percentiles) {
		final List<OrderPercentile> percentileValues = new ArrayList<OrderPercentile>(); // TODO: this is inefficient. Cache it?
		
		for (int i = 0; i < percentiles.length; i++) {
			double percentile = percentiles[i];
			percentileValues.add(new OrderPercentile(percentile, 0d, 0d));
		}
		
		return percentileValues;
	}
	
	private static void setSnapshotValues(ServerSnapshot entry, List<OrderPercentile> percentiles, final String orderType, final int startIndex) {
		int index = startIndex;
		for (OrderPercentile percentile : percentiles) {
			entry.set(index, new Sample(entry.timestamp, percentile.volume));
			index++;
		}
	}
}
