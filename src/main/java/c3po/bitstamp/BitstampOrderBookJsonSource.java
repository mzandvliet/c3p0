package c3po.bitstamp;

import java.util.HashMap;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import c3po.*;
import c3po.node.INode;
import c3po.utils.JsonReader;
import c3po.utils.SignalMath;

public class BitstampOrderBookJsonSource extends BitstampOrderBookSource implements INode {
	private static final Logger LOGGER = LoggerFactory.getLogger(BitstampOrderBookJsonSource.class);
	
	private final String url;
	
	public BitstampOrderBookJsonSource(long timestep, long interpolationTime, String url) {
		super(timestep, interpolationTime);
		this.url = url;
	}
	
	@Override
	public boolean open() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean close() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isEmpty() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void pollServer(long clientTimestamp) {
		parseJson();
	}

	private static final int HIGHEST_PERCENTILE = 99;
	private static final int LOWEST_PERCENTILE = 95;
	
	private void parseJson() {		
		try {
			JSONObject json = JsonReader.readJsonFromUrl(url);
			
			long serverTimestamp = json.getLong("timestamp") * 1000;
			
			// Early out if we already have the latest snapshot from the server
			long lastServerTimestamp = buffer.size() != 0 ? buffer.get(buffer.size()-1).timestamp : 0;
			if (serverTimestamp == lastServerTimestamp) {
				LOGGER.debug("Skipping duplicate server entry for timestamp " + serverTimestamp);
				return;
			}
			
			LOGGER.debug("Parsing Orderbook at " + serverTimestamp + "...");
			
			JSONArray bids = json.getJSONArray("bids");
			JSONArray asks = json.getJSONArray("asks");
			
			double totalBidVolume = calculateTotalVolume(bids);
			double totalAskVolume = calculateTotalVolume(asks);
			
			HashMap<Integer, Double> bidPercentiles = calculatePercentiles(bids, totalBidVolume);
			HashMap<Integer, Double> askPercentiles = calculatePercentiles(asks, totalAskVolume);
			
			ServerSnapshot entry = new ServerSnapshot(serverTimestamp, 12);
			
			entry.set(OrderBookSignal.VOLUME_BID.ordinal(), new Sample(serverTimestamp, totalBidVolume));
			entry.set(OrderBookSignal.VOLUME_ASK.ordinal(), new Sample(serverTimestamp, totalAskVolume));
			
			setServerEntryValues(entry, bidPercentiles, "BID");
			setServerEntryValues(entry, askPercentiles, "ASK");
			
			buffer.add(entry);
		} catch (Exception e) {
			/* TODO
			 * - catch json, io and connection exceptions specifically
			 * - retry a number of times!
			 */
			LOGGER.warn("Failed to fetch or parse json, reason: " + e);
		}
	}

	private static HashMap<Integer, Double> calculatePercentiles(final JSONArray orders, final double totalVolume) {
		final HashMap<Integer, Double> percentiles = new HashMap<Integer, Double>(); // TODO: this is inefficient. Cache it?
		
		int currentPercentile = HIGHEST_PERCENTILE;
		double volumeParsed = 0d;
		double lastPrice = 0d;
		
		for (int i = 0; i < orders.length(); i++) {
			final JSONArray order = orders.getJSONArray(i);
			final double price = order.getDouble(0);
			final double volume = order.getDouble(1);
			
			double percentileVolumeThreshold = totalVolume * ((100 - currentPercentile) / 100d);
			
			if (volumeParsed + volume > percentileVolumeThreshold) {				
//				final double lerp = (percentileVolumeThreshold - volumeParsed) / volume;
//				final double lerp = SignalMath.interpolateInverse(volumeParsed, volumeParsed + volume, percentileVolumeThreshold);
//				final double percentilePrice = SignalMath.interpolate(lastPrice, price, lerp);
				final double percentilePrice = price;
				percentiles.put(new Integer(currentPercentile), new Double(percentilePrice));
				currentPercentile -= 1;
			}
			
			volumeParsed += volume;
			lastPrice = price;
			
			if (currentPercentile < LOWEST_PERCENTILE)
				break;
		}
		
		return percentiles;
	}
	
	private static void setServerEntryValues(ServerSnapshot entry, HashMap<Integer, Double> percentiles, String orderType) {
		for (Entry<Integer, Double> percentile : percentiles.entrySet()) {
			final int signalIndex = getOrderBookSignalIndex(orderType, percentile);
			entry.set(signalIndex, new Sample(entry.timestamp, percentile.getValue().doubleValue()));
		}
	}

	private static int getOrderBookSignalIndex(String orderType, Entry<Integer, Double> percentile) {
		return OrderBookSignal.valueOf(String.format("P%s_%s", percentile.getKey().intValue(), orderType)).ordinal(); // TODO: Jezus...
	}
	
	private double calculateTotalVolume(JSONArray orders) {
		double totalVolume = 0;
		for (int i = 0; i < orders.length(); i++) {
			final JSONArray order = orders.getJSONArray(i);
			final double volume = order.getDouble(1);
			totalVolume += volume;
		}
		return totalVolume;
	}
}
