package c3po;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

public class BitstampTickerJsonSource implements INode {
	private final int numSignals = 6;
	private final String url;
	private OutputSignal[] signals;
	private long lastTick = -1;
	
	public BitstampTickerJsonSource(String url) {
		this.url = url;
		this.signals = new OutputSignal[numSignals];
		for (int i = 0; i < numSignals; i++) {
			this.signals[i] = new OutputSignal(this);
		}
	}
		
	@Override
	public int getNumOutputs() {
		return signals.length;
	}
	
	@Override
	public ISignal getOutput(int i) {
		return signals[i];
	}
	
	public enum SignalName {
		LAST,
	    HIGH,
	    LOW,
	    VOLUME,
	    BID,
	    ASK
	}
	
	@Override
	public void tick(long tick) {
		if (tick >= lastTick) {
			parseJson();
			lastTick = tick;
		}
	}
	
	private void parseJson() {
		try {
			JSONObject json = JsonReader.readJsonFromUrl(url);
			long timestamp = json.getLong("timestamp");
    		signals[0].setSample(new Sample(timestamp, json.getDouble("last")));
    		signals[1].setSample(new Sample(timestamp, json.getDouble("high")));
    		signals[2].setSample(new Sample(timestamp, json.getDouble("low")));
    		signals[3].setSample(new Sample(timestamp, json.getDouble("volume")));
    		signals[4].setSample(new Sample(timestamp, json.getDouble("bid")));
    		signals[5].setSample(new Sample(timestamp, json.getDouble("ask")));
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
