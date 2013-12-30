package c3po.bitstamp;

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
		return true;
	}

	@Override
	public boolean close() {
		return true;
	}

	@Override
	public boolean isEmpty() {
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
			
			ServerSnapshot entry = new ServerSnapshot(serverTimestamp, 6);
    		entry.set(TickerSignal.LAST.ordinal(), new Sample(serverTimestamp, json.getDouble("last")));
    		entry.set(TickerSignal.HIGH.ordinal(), new Sample(serverTimestamp, json.getDouble("high")));
    		entry.set(TickerSignal.LOW.ordinal(), new Sample(serverTimestamp, json.getDouble("low")));
    		entry.set(TickerSignal.VOLUME.ordinal(), new Sample(serverTimestamp, json.getDouble("volume")));
    		entry.set(TickerSignal.BID.ordinal(), new Sample(serverTimestamp, json.getDouble("bid")));
    		entry.set(TickerSignal.ASK.ordinal(), new Sample(serverTimestamp, json.getDouble("ask")));
    		
    		ServerSnapshot lastEntry = buffer.size() > 0 ? buffer.get(buffer.size()-1) : null; 
			if (!entry.equals(lastEntry))
				buffer.add(entry);
    		
		} catch (Exception e) {
			/* TODO
			 * - catch json, io and connection exceptions specifically
			 * - retry a number of times!
			 */
			LOGGER.warn("Failed to fetch or parse json, reason: " + e);
		}
	}
}
