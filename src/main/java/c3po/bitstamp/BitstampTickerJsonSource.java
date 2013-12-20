package c3po.bitstamp;

import java.io.IOException;
import org.json.JSONException;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import c3po.*;
import c3po.node.INode;
import c3po.utils.JsonReader;

public class BitstampTickerJsonSource extends BitstampTickerSource implements INode {
	private static final Logger LOGGER = LoggerFactory.getLogger(BitstampTickerJsonSource.class);
	
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
    		entry.set(SignalName.LAST.ordinal(), new Sample(serverTimestamp, json.getDouble("last")));
    		entry.set(SignalName.HIGH.ordinal(), new Sample(serverTimestamp, json.getDouble("high")));
    		entry.set(SignalName.LOW.ordinal(), new Sample(serverTimestamp, json.getDouble("low")));
    		entry.set(SignalName.VOLUME.ordinal(), new Sample(serverTimestamp, json.getDouble("volume")));
    		entry.set(SignalName.BID.ordinal(), new Sample(serverTimestamp, json.getDouble("bid")));
    		entry.set(SignalName.ASK.ordinal(), new Sample(serverTimestamp, json.getDouble("ask")));
    		
    		ServerSampleEntry lastEntry = buffer.size() > 0 ? buffer.get(buffer.size()-1) : null; 
			if (!entry.equals(lastEntry))
				buffer.add(entry);
    		
		} catch (Exception e) {
			/* TODO
			 * - catch json, io and connection exceptions specifically
			 * - retry a number of times!
			 */
			LOGGER.warn("Failed to fetch json, reason: " + e);
		}
	}
}
