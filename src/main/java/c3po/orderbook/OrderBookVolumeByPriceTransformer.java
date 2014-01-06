package c3po.orderbook;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import c3po.events.AbstractEventSource;
import c3po.events.IEventListener;

public class OrderBookVolumeByPriceTransformer extends AbstractEventSource<OrderBookPercentileSnapshot> implements IEventListener<OrderBookSample> {
	private static final Logger LOGGER = LoggerFactory.getLogger(OrderBookVolumeByPriceTransformer.class);
	
	private final double[] percentiles;
	private final double priceRange;
	
	public OrderBookVolumeByPriceTransformer(double[] percentiles, double priceRange) {
		this.percentiles = percentiles;
		this.priceRange = priceRange;
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
		
		double highestBid = bids.get(0).price;
		double lowestAsk = asks.get(0).price;
		
		// Calculate percentiles and format them to signals
		
		List<OrderPercentile> bidPercentiles = calculateBidPercentiles(bids, highestBid - priceRange, highestBid, percentiles);
		List<OrderPercentile> askPercentiles = calculateAskPercentiles(asks, lowestAsk, lowestAsk + priceRange, percentiles);
		
		return new OrderBookPercentileSnapshot(orderbookSample.timestamp, highestBid, lowestAsk, bidPercentiles, askPercentiles);
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
	
	private static List<OrderPercentile> calculateBidPercentiles(final List<Order> orders, final double lowestPrice, final double highestPrice, double[] percentiles) {
		final List<OrderPercentile> percentileValues = new ArrayList<OrderPercentile>();
		
		for (int i = 0; i < percentiles.length; i++) {
			percentileValues.add(new OrderPercentile(percentiles[i]));
		}
		
		int percentileIndex = 0;
		double aggregatedVolume = 0d;
		
		for (int i = 0; i < orders.size(); i++) {
			Order order = orders.get(i);
			
			OrderPercentile orderPercentile = percentileValues.get(percentileIndex);
			double percentilePriceThreshold = lowestPrice + (highestPrice - lowestPrice) * (orderPercentile.percentile / 100d);
			
			aggregatedVolume += order.volume;
			
			if (order.price < percentilePriceThreshold) {
				orderPercentile.price = percentilePriceThreshold;
				orderPercentile.volume = aggregatedVolume;
				
				LOGGER.debug(orderPercentile.percentile + ", " + order.price + ", "+ aggregatedVolume);
				
				//aggregatedVolume = 0d;
				percentileIndex++;
			}

			if (percentileIndex >= percentiles.length)
				break;
		}
		
		return percentileValues;
	}
	
	private static List<OrderPercentile> calculateAskPercentiles(final List<Order> orders, final double lowestPrice, final double highestPrice, double[] percentiles) {
		final List<OrderPercentile> percentileValues = new ArrayList<OrderPercentile>();
		
		for (int i = 0; i < percentiles.length; i++) {
			percentileValues.add(new OrderPercentile(percentiles[i]));
		}
		
		int percentileIndex = 0;
		double aggregatedVolume = 0d;
		
		for (int i = 0; i < orders.size(); i++) {
			Order order = orders.get(i);
			
			OrderPercentile orderPercentile = percentileValues.get(percentileIndex);
			double percentilePriceThreshold = lowestPrice + (highestPrice-lowestPrice) * ((100d - orderPercentile.percentile) / 100d);
			
			aggregatedVolume += order.volume;
			
			if (order.price > percentilePriceThreshold) {
				orderPercentile.price = percentilePriceThreshold;
				orderPercentile.volume = aggregatedVolume;
				
				LOGGER.debug(orderPercentile.percentile + ", " + order.price + ", "+ aggregatedVolume);
				
				//aggregatedVolume = 0d;
				percentileIndex++;
			}

			if (percentileIndex >= percentiles.length)
				break;
		}
		
		return percentileValues;
	}
}
