package c3po.bitstamp;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import c3po.*;

public class BitstampSimulationTickerDbSource extends BitstampTickerSource implements INonRealtimeSource {
	private static final Logger LOGGER = LoggerFactory.getLogger(BitstampSimulationTickerDbSource.class);
	private static final int MAXRETRIES = 3;

	private DbConnection connection;
	
	private List<ServerSampleEntry> data;
	
	private long simulationStartTime;
	private long simulationEndTime;
	
	private int lastDataIndex;
	
	public BitstampSimulationTickerDbSource(long timestep, long interpolationTime, DbConnection connection, long dataStartTime, long dataEndTime) {
		  super(timestep, interpolationTime);
		  
		  this.connection = connection;
		  data = new ArrayList<ServerSampleEntry>();
		  fetchDataFromDatabase(dataStartTime, dataEndTime);
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
		
		boolean done = false;
	    while(!done && lastDataIndex < data.size()) {	   
	    	ServerSampleEntry newest = buffer.peek();
			long newestTimeInBuffer = newest != null ? newest.timestamp : clientTimestamp;
			
			if (newestTimeInBuffer < serverTime) {
		    	buffer.add(data.get(lastDataIndex));
		    	lastDataIndex++;
	    	} else {
	    		done = true;
	    	}
	    }
	}
	
	private void fetchDataFromDatabase(long start, long end) {
		data.clear();
		
		try {
			// Get all entries from the simulation start time to the simulation end time
		    String query = "select * from bitstamp_ticker WHERE `timestamp` BETWEEN " + start / 1000  + " AND " + end / 1000 + " ORDER BY timestamp ASC";
		    ResultSet resultSet = connection.executeQueryWithRetries(query, MAXRETRIES);
		    
		    // Add them all to the buffer
		    while(resultSet.next()) {
		    	long entryTime = resultSet.getLong("timestamp") * 1000;
		    	ServerSampleEntry entry = new ServerSampleEntry(entryTime, 6);
		    	
		    	entry.set(SignalName.LAST.ordinal(), new Sample(entryTime, resultSet.getDouble("last")));
				entry.set(SignalName.HIGH.ordinal(), new Sample(entryTime, resultSet.getDouble("high")));
				entry.set(SignalName.LOW.ordinal(), new Sample(entryTime, resultSet.getDouble("low")));
				entry.set(SignalName.VOLUME.ordinal(), new Sample(entryTime, resultSet.getDouble("volume")));
				entry.set(SignalName.BID.ordinal(), new Sample(entryTime, resultSet.getDouble("bid")));
				entry.set(SignalName.ASK.ordinal(), new Sample(entryTime, resultSet.getDouble("ask")));
		    	
		    	data.add(entry);
		    }
		    
		    resultSet.close();
		    
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
		seek(simulationStartTime);
	}
	
	private void seek(long simulationStartTime) {
		for (int i = 0; i < data.size(); i++) {
			if (data.get(i).timestamp >= simulationStartTime) {
				lastDataIndex = i;
				return;
			}
		}
	}

	@Override
	public void setSimulationRange(long startTime, long endTime) {
		
		this.simulationStartTime = startTime;
		this.simulationEndTime = endTime;
		reset();
	}
}
