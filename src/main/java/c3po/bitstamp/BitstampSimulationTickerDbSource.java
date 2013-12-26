package c3po.bitstamp;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import c3po.*;
import c3po.db.DbTimeseriesSource;

public class BitstampSimulationTickerDbSource extends BitstampTickerSource implements INonRealtimeSource {
	private static final Logger LOGGER = LoggerFactory.getLogger(BitstampSimulationTickerDbSource.class);
	private static final int MAXRETRIES = 3;

	private DbConnection connection;
		
	private long simulationStartTime;
	private long simulationEndTime;
	
	private int lastDataIndex;
	
	private DbTimeseriesSource source;
	
	public BitstampSimulationTickerDbSource(long timestep, long interpolationTime, DbConnection connection, long dataStartTime, long dataEndTime) {
		  super(timestep, interpolationTime);
		  
		  this.connection = connection;
		  
		  this.source = new DbTimeseriesSource(timestep, connection, "bitstamp_ticker", Arrays.asList("last", "high", "low", "volume", "bid", "ask"));
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
		
		this.simulationStartTime = startTime;
		this.simulationEndTime = endTime;
		reset();
	}
}
