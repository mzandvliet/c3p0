package c3po.bitstamp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import c3po.AbstractTickable;
import c3po.events.AbstractEventSource;
import c3po.events.IEventListener;
import c3po.events.IEventSource;
import c3po.orderbook.Order;
import c3po.orderbook.OrderBookSample;
import c3po.utils.JsonReader;

public class BitstampOrderBookJsonEventSource extends AbstractTickable implements IEventSource<OrderBookSample> {
	private static final Logger LOGGER = LoggerFactory.getLogger(BitstampOrderBookJsonEventSource.class);
	
	private final String url;
	private final AbstractEventSource<OrderBookSample> eventSource;
	private long lastServerTimestamp;
	
	public BitstampOrderBookJsonEventSource(long timestep, String url) {
		super(timestep);
		this.url = url;
		this.eventSource = new AbstractEventSource<OrderBookSample>();
	}
	
	@Override
	public void addListener(IEventListener<OrderBookSample> listener) {
		eventSource.addListener(listener);
	}
	
	@Override
	public void removeListener(IEventListener<OrderBookSample> listener) {
		eventSource.removeListener(listener);
	}
	
	@Override
	public void update(long tick) {
		OrderBookSample sample = fetchJson();
		if (sample != null)
			eventSource.produce(sample);
	}

	private OrderBookSample fetchJson() {
		// TODO: Catch connection errors, attempt retries
		
		try {
			// Get latest orderbook through json
			JSONObject json = JsonReader.readJsonFromUrl(url);
			long serverTimestamp = json.getLong("timestamp") * 1000;
			
			// Early out if we already have the latest snapshot from the server
			if (serverTimestamp == lastServerTimestamp) {
				LOGGER.debug("Skipping duplicate server entry for timestamp " + serverTimestamp);
				return null;
			}
			lastServerTimestamp = serverTimestamp;
			
			LOGGER.debug("Parsing Orderbook at " + serverTimestamp + "...");
			
			List<Order> bids = parseOrders(json.getJSONArray("bids"));
			List<Order> asks = parseOrders(json.getJSONArray("asks"));
			
			return new OrderBookSample(serverTimestamp, bids, asks);
		} catch (JSONException e) {
			LOGGER.warn("Failed to parse json, reason: " + e);
		} catch (IOException e) {
			LOGGER.warn("Failed to fetch json, reason: " + e);
		}

		return null;
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