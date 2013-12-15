package c3po.bitstamp;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import c3po.*;
import c3po.node.INode;
import c3po.utils.JsonReader;

public class BitstampTickerJsonSource extends BitstampTickerSource implements INode {
	private final String url;
	
	public BitstampTickerJsonSource(long timestep, long interpolationTime, String url) {
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
			
			ServerSampleEntry entry = new ServerSampleEntry(serverTimestamp, 6);
    		entry.set(0, new Sample(serverTimestamp, json.getDouble("last")));
    		entry.set(1, new Sample(serverTimestamp, json.getDouble("high")));
    		entry.set(2, new Sample(serverTimestamp, json.getDouble("low")));
    		entry.set(3, new Sample(serverTimestamp, json.getDouble("volume")));
    		entry.set(4, new Sample(serverTimestamp, json.getDouble("bid")));
    		entry.set(5, new Sample(serverTimestamp, json.getDouble("ask")));
    		
    		buffer.add(entry);
    		
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	
}
