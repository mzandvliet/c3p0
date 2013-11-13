package c3po;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import org.json.JSONException;
import org.json.JSONObject;

public class BitstampTickerSignalSource implements ISignalSource {
	private final String jsonUrl = "http://www.bitstamp.net/api/ticker/";
	
	private Signal latest;
	private long lastTick = -1;
		
	public Signal get(long tick) {
		update(tick);
		return latest;
	}
	
	private void update(long tick) {
		if (tick >= lastTick) {
			latest = getLatest();
			lastTick = tick;
		}
	}

	private Signal getLatest() {
		Signal latest;
		
		try {
			JSONObject json = JsonReader.readJsonFromUrl(jsonUrl);
			latest = new Signal(json.getLong("timestamp"), json.getDouble("last"));
		} catch (JSONException e) {
			latest = new Signal();
			e.printStackTrace();
		} catch (IOException e) {
			latest = new Signal();
			e.printStackTrace();
		}
		
		return latest;
	}
}
