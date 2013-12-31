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
			// Get latest orderbook through json
			
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
			
			double totalBidVolume = calculateTotalOrderVolume(bids);
			double totalAskVolume = calculateTotalOrderVolume(asks);
			
			// Print some general market stats, because hey they're cool! (TODO: totally do this somewhere else)
			
			double avgBidVolume = totalBidVolume / (double)bids.length();
			double avgAskVolume = totalAskVolume / (double)asks.length();
			LOGGER.debug(String.format("Market Stats:\nbids: [num %s, sum $%,.2f, average $%,.2f]\nasks: [num %s, sum $%,.2f, average $%,.2f]\ntotal bid/ask: %,.2f, average bid/ask: %,.2f",
					bids.length(),
					totalBidVolume,
					avgBidVolume,
					asks.length(),
					totalAskVolume,
					avgAskVolume,
					totalBidVolume / totalAskVolume,
					avgBidVolume / avgAskVolume
			));
			
			// Calculate percentiles and format them to signals
			
			HashMap<Integer, Double> bidPercentiles = calculatePercentiles(bids, totalBidVolume);
			HashMap<Integer, Double> askPercentiles = calculatePercentiles(asks, totalAskVolume);
			
			ServerSnapshot entry = new ServerSnapshot(serverTimestamp, 2 + 2 * numPercentiles);
			
			entry.set(0, new Sample(serverTimestamp, totalBidVolume));
			entry.set(1, new Sample(serverTimestamp, totalAskVolume));
			
			setServerEntryValues(entry, bidPercentiles, "BID", 2);
			setServerEntryValues(entry, askPercentiles, "ASK", 2 + numPercentiles);
			
			buffer.add(entry);
		} catch (JSONException e) {
			LOGGER.warn("Failed to parse json, reason: " + e);
		} catch (IOException e) {
			LOGGER.warn("Failed to fetch json, reason: " + e);
		}
		// TODO: Catch connection errors, attempt retries
	}

	private static HashMap<Integer, Double> calculatePercentiles(final JSONArray orders, final double totalVolume) {
		final HashMap<Integer, Double> percentileValues = new HashMap<Integer, Double>(); // TODO: this is inefficient. Cache it?
		
		int percentileIndex = 0;
		double volumeParsed = 0d;
		double lastPrice = 0d;
		
		for (int i = 0; i < orders.length(); i++) {
			final JSONArray order = orders.getJSONArray(i);
			final double price = order.getDouble(0);
			final double volume = order.getDouble(1);
			
			int currentPercentile = percentiles[percentileIndex];
			double percentileVolumeThreshold = totalVolume * ((100 - currentPercentile) / 100d);
			
			if (volumeParsed + volume > percentileVolumeThreshold) {
//				double lerp = SignalMath.interpolateInverse(volumeParsed, volumeParsed + volume, percentileVolumeThreshold);
//				double percentilePrice = SignalMath.interpolate(lastPrice, price, lerp);
				double percentilePrice = price;
				
				percentileValues.put(new Integer(currentPercentile), new Double(percentilePrice));
				LOGGER.debug(currentPercentile + ", " + percentilePrice);
				percentileIndex++;
			}
			
			volumeParsed += volume;
			lastPrice = price;
			
			if (percentileIndex >= numPercentiles)
				break;
		}
		
		return percentileValues;
	}
	
	private static void setServerEntryValues(ServerSnapshot entry, HashMap<Integer, Double> percentiles, String orderType, int startIndex) {
		int index = startIndex;
		for (Entry<Integer, Double> percentile : percentiles.entrySet()) {
			entry.set(index, new Sample(entry.timestamp, percentile.getValue().doubleValue()));
			index++;
		}
	}
	
	private static String getOrderBookSignalName(String orderType, int percentile) {
		return String.format("P%s_%s", percentile, orderType);
	}
	
	private double calculateTotalOrderVolume(JSONArray orders) {
		double totalVolume = 0;
		for (int i = 0; i < orders.length(); i++) {
			final JSONArray order = orders.getJSONArray(i);
			final double volume = order.getDouble(1);
			totalVolume += volume;
		}
		return totalVolume;
	}
}
