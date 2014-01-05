package c3po.orderbook;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import c3po.events.AbstractEventSource;
import c3po.events.IEventListener;

public class OrderBookPricePercentileTransformer extends AbstractEventSource<OrderBookPercentileSnapshot> implements IEventListener<OrderBookSample> {
	private static final Logger LOGGER = LoggerFactory.getLogger(OrderBookPricePercentileTransformer.class);
	
	private final double[] percentiles;
	
	public OrderBookPricePercentileTransformer(double[] percentiles) {
		this.percentiles = percentiles;
	}

	@Override
	public void onEvent(OrderBookSample sample) {
		produce(transform(sample));
	}

	public OrderBookPercentileSnapshot transform(OrderBookSample orderbookSample) {
		List<Order> bids = orderbookSample.bids;
		List<Order> asks = orderbookSample.asks;
		
		removeStatisticallyDeviantOrders(bids, 0.95d);
		removeStatisticallyDeviantOrders(asks, 0.95d);
		
		double lowestBid = bids.get(bids.size()-1).price;
		double highestBid = bids.get(0).price;
		double lowestAsk = asks.get(0).price;
		double highestAsk = asks.get(asks.size()-1).price;
		
		// Calculate percentiles and format them to signals
		
		List<OrderPercentile> bidPercentiles = calculateBidPercentiles(bids, lowestBid, highestBid, percentiles);
		List<OrderPercentile> askPercentiles = calculateAskPercentiles(asks, lowestAsk, highestAsk, percentiles);
		
		return new OrderBookPercentileSnapshot(orderbookSample.timestamp, lowestBid, highestBid, lowestAsk, highestAsk, bidPercentiles, askPercentiles);
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

	 *  High percentiles should map to orders close to LAST, low percentiles to orders far away from LAST.
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
		final List<OrderPercentile> percentileValues = new ArrayList<OrderPercentile>();
		
		for (int i = 0; i < percentiles.length; i++) {
			percentileValues.add(new OrderPercentile(percentiles[i]));
		}
		
		int percentileIndex = 0;
		double aggregatedVolume = 0d;
		
		for (int i = 0; i < orders.size(); i++) {
			Order order = orders.get(i);
			
			OrderPercentile orderPercentile = percentileValues.get(percentileIndex);
			double percentilePriceThreshold = minPrice + (maxPrice-minPrice) * (orderPercentile.percentile / 100d);
			
			aggregatedVolume += order.volume;
			
			if (order.price > percentilePriceThreshold) {
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
}
