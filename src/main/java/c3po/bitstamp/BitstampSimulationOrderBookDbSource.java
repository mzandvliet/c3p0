package c3po.bitstamp;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import c3po.*;
import c3po.db.DbTimeseriesSource;

public class BitstampSimulationOrderBookDbSource extends BitstampOrderBookSource implements INonRealtimeSource {
	private static final Logger LOGGER = LoggerFactory.getLogger(BitstampSimulationOrderBookDbSource.class);

	private long simulationStartTime;
	private long simulationEndTime;
	
	private DbTimeseriesSource source;
	
	public BitstampSimulationOrderBookDbSource(long timestep, long interpolationTime, DbConnection connection, long dataStartTime, long dataEndTime) {
		  super(timestep, interpolationTime);
		  
		  this.source = new DbTimeseriesSource(timestep, connection, "bitstamp_order_book", Arrays.asList(
				  "volume_ask", "volume_bid", 
				  "p99_bid", "p98_bid", "p97_bid", "p96_bid", "p95_bid",
				  "p99_ask", "p98_ask", "p97_ask", "p96_ask", "p95_ask"));
		  
		  this.source.fetchDataFromDatabase(dataStartTime, dataEndTime);

		  setSimulationRange(dataStartTime, dataEndTime); // Default the simulation to the full range of data
	}
	
	@Override
	protected void pollServer(long clientTimestamp) {
    	readToCurrent(clientTimestamp);	    	
	}
	
	private void readToCurrent(long clientTimestamp) {
		tryGetNewEntry(clientTimestamp);
	}
	
	private void tryGetNewEntry(long clientTimestamp) {
		long serverTime = clientTimestamp + interpolationTime;
		
		List<ServerSampleEntry> newSamples = this.source.getNewSamples(serverTime);
		for(ServerSampleEntry sample : newSamples) {
			buffer.add(sample);
		}
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
	public long getStartTime() {
		return simulationStartTime;
	}

	@Override
	public long getEndTime() {
		return simulationEndTime;
	}

	@Override
	public void reset() {
		super.reset();
		source.reset();
	}
	

	@Override
	public void setSimulationRange(long startTime, long endTime) {
		this.simulationStartTime = startTime - interpolationTime;
		this.simulationEndTime = endTime;
		reset();
	}
}
