package c3po.bitstamp;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
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
			
			ServerSnapshot entry = new ServerSnapshot(serverTimestamp, 2 + 2 * 9);
			
			entry.set(OrderBookSignal.VOLUME_BID.ordinal(), new Sample(serverTimestamp, totalBidVolume));
			entry.set(OrderBookSignal.VOLUME_ASK.ordinal(), new Sample(serverTimestamp, totalAskVolume));
			
			setServerEntryValues(entry, bidPercentiles, "BID");
			setServerEntryValues(entry, askPercentiles, "ASK");
			
			buffer.add(entry);
		} catch (JSONException e) {
			/* TODO
			 * - catch json, io and connection exceptions specifically
			 * - retry a number of times!
			 */
			LOGGER.warn("Failed to parse json, reason: " + e);
		} catch (IOException e) {
			LOGGER.warn("Failed to fetch json, reason: " + e);
		}
	}
	
	private static final int[] percentiles = { 99, 98, 97, 96, 95, 90, 85, 80, 75 };
	private static final int numPercentiles = percentiles.length;

	private static HashMap<Integer, Double> calculatePercentiles(final JSONArray orders, final double totalVolume) {
		final HashMap<Integer, Double> percentileValues = new HashMap<Integer, Double>(); // TODO: this is inefficient. Cache it?
		
		int percentileIndex = 0;
		double volumeParsed = 0d;
		
		for (int i = 0; i < orders.length(); i++) {
			final JSONArray order = orders.getJSONArray(i);
			final double price = order.getDouble(0);
			final double volume = order.getDouble(1);
			
			int currentPercentile = percentiles[percentileIndex];
			double percentileVolumeThreshold = totalVolume * ((100 - currentPercentile) / 100d);
			
			if (volumeParsed + volume > percentileVolumeThreshold) {								
				percentileValues.put(new Integer(currentPercentile), new Double(price));
				LOGGER.debug(currentPercentile + ", " + price);
				percentileIndex++;
			}
			
			volumeParsed += volume;
			
			if (percentileIndex >= numPercentiles)
				break;
		}
		
		return percentileValues;
	}
	
	private static void setServerEntryValues(ServerSnapshot entry, HashMap<Integer, Double> percentiles, String orderType) {
		for (Entry<Integer, Double> percentile : percentiles.entrySet()) {
			final int signalIndex = getOrderBookSignalIndex(orderType, percentile);
			entry.set(signalIndex, new Sample(entry.timestamp, percentile.getValue().doubleValue()));
		}
	}
	
	private static String getOrderBookSignalName(String orderType, int percentile) {
		return String.format("P%s_%s", percentile, orderType);
	}

	private static int getOrderBookSignalIndex(String orderType, Entry<Integer, Double> percentile) {
		return OrderBookSignal.valueOf(getOrderBookSignalName(orderType, percentile.getKey().intValue())).ordinal();
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
