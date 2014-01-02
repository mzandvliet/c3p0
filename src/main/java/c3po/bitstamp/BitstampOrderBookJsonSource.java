package c3po.bitstamp;

import java.io.IOException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import c3po.*;
import c3po.orderbook.IOrderBookSource;
import c3po.orderbook.Order;
import c3po.orderbook.OrderBookSample;
import c3po.utils.JsonReader;

public class BitstampOrderBookJsonSource extends AbstractTickable implements IOrderBookSource {
	private static final Logger LOGGER = LoggerFactory.getLogger(BitstampOrderBookJsonSource.class);
	
	private final String url;
	private OrderBookSample lastSample;
	
	public BitstampOrderBookJsonSource(long timestep, String url) {
		super(timestep);
		this.url = url;
	}
	
	@Override
	public OrderBookSample getSample(long tick) {
		tick(tick); // TODO: Have to do this manually since we don't have any output signals that call it for us. Indicates messy design.
		
		return lastSample;
	}
	
	@Override
	public void onNewTick(long tick) {
		fetchJson();
	}

	private void fetchJson() {		
		try {
			// Get latest orderbook through json
			JSONObject json = JsonReader.readJsonFromUrl(url);
			long serverTimestamp = json.getLong("timestamp") * 1000;
			
			// Early out if we already have the latest snapshot from the server
			long lastServerTimestamp = lastSample != null ? lastSample.timestamp : 0;
			if (serverTimestamp == lastServerTimestamp) {
				LOGGER.debug("Skipping duplicate server entry for timestamp " + serverTimestamp);
				return;
			}
			
			LOGGER.debug("Parsing Orderbook at " + serverTimestamp + "...");
			
			List<Order> bids = parseOrders(json.getJSONArray("bids"));
			List<Order> asks = parseOrders(json.getJSONArray("asks"));
			
			lastSample = new OrderBookSample(serverTimestamp, bids, asks);
		} catch (JSONException e) {
			LOGGER.warn("Failed to parse json, reason: " + e);
		} catch (IOException e) {
			LOGGER.warn("Failed to fetch json, reason: " + e);
		}
		// TODO: Catch connection errors, attempt retries
	}

	private List<Order> parseOrders(JSONArray jsonArray) {
		ArrayList<Order> orders = new ArrayList<Order>(jsonArray.length());
		
		for (int i = 0; i < jsonArray.length(); i++) {
			JSONArray order = jsonArray.getJSONArray(i);
			orders.add(new Order(order.getDouble(0), order.getDouble(1)));
		}
		
		return orders;
	}
}
