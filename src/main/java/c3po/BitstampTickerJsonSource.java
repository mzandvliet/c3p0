package c3po;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

public class BitstampTickerJsonSource extends BitstampTickerSource implements INode {
	private final String url;
	
	public BitstampTickerJsonSource(String url) {
		super();
		this.url = url;
	}
	
	@Override
	public void open() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isEmpty() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onNewTick(long tick) {
		parseJson();
		
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
