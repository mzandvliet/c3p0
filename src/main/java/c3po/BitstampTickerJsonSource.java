package c3po;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

public class BitstampTickerJsonSource implements ISignalSource {
	private final String url = "http://www.bitstamp.net/api/ticker/";
	
	private Signal latest;
	private long lastTick = -1;
		
	public Signal getLatest(long tick) {
		update(tick);
		return Signal.copy(latest);
	}
	
	private void update(long tick) {
		if (tick >= lastTick) {
			latest = parseJson();
			lastTick = tick;
		}
	}

	private Signal parseJson() {
		try {
			JSONObject json = JsonReader.readJsonFromUrl(url);
			return new Signal(json.getLong("timestamp"), json.getDouble("last"));
		} catch (JSONException e) {
			e.printStackTrace();
			return Signal.none;
		} catch (IOException e) {
			e.printStackTrace();
			return Signal.none;
		}
	}
}
