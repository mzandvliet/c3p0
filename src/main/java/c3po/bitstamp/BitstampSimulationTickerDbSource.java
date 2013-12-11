package c3po.bitstamp;

import java.net.InetSocketAddress;
import java.sql.*;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import c3po.*;

public class BitstampSimulationTickerDbSource extends BitstampTickerSource implements INonRealtimeSource {
	private static final Logger LOGGER = LoggerFactory.getLogger(BitstampSimulationTickerDbSource.class);
	private static final int MAXRETRIES = 3;
	
	private final long startTime;
	private final long endTime;
	private DbConnection connection;
	
	private ArrayList<ServerSampleEntry> history;
	private int lastHistoryIndex;
	
	public BitstampSimulationTickerDbSource(long timestep, long interpolationTime, DbConnection connection, long startTime, long endTime) {
		  super(timestep, interpolationTime);
		  
		  this.connection = connection;
		  this.startTime = startTime;
		  this.endTime = endTime;
		  
		  history = new ArrayList<ServerSampleEntry>();
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
	    while(!done && lastHistoryIndex < history.size()) {	   
	    	ServerSampleEntry newest = buffer.peek();
			long newestTimeInBuffer = newest != null ? newest.timestamp : clientTimestamp;
			
			if (newestTimeInBuffer < serverTime) {
		    	buffer.add(history.get(lastHistoryIndex));
		    	lastHistoryIndex++;
	    	} else {
	    		done = true;
	    	}
	    }
	}

	@Override
	public boolean open() {
		connection.open();
		
		try {
			// Get all entries from the simulation start time to the simulation end time
		    String query = "select * from bitstamp_ticker WHERE `timestamp` BETWEEN " + startTime / 1000  + " AND " + endTime / 1000 + " ORDER BY timestamp ASC";
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
		    	
		    	history.add(entry);
		    }
		    
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		return true;
	}

	@Override
	public boolean close() {
		return connection.close();
	}

	@Override
	public boolean isEmpty() {
		return false;
	}

	@Override
	public long getStartTime() {
		return startTime;
	}

	@Override
	public long getEndTime() {
		return endTime;
	}

	@Override
	public void reset() {
		super.reset();
		
		lastHistoryIndex = 0;
		buffer.clear();
		
		for (OutputSignal signal : signals) {
			signal.setSample(Sample.none);
		}
	}
}
