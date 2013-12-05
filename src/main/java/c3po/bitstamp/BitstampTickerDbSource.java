package c3po.bitstamp;

import java.net.InetSocketAddress;
import java.sql.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import c3po.*;
import c3po.bitstamp.BitstampTickerSource.SignalName;

/**
 * Work in progress in using the database as source
 */
public class BitstampTickerDbSource extends BitstampTickerSource {
	private static final Logger LOGGER = LoggerFactory.getLogger(BitstampTickerDbSource.class);
	private static final int MAXRETRIES = 3;

	private DbConnection connection = null;
	
	public BitstampTickerDbSource(long timestep, long interpolationTime, DbConnection connection) throws ClassNotFoundException, SQLException {
		  super(timestep, interpolationTime);
		  this.connection = connection;
	}
	
	@Override
	protected void pollServer(long clientTimestamp) {
    	readToCurrent(clientTimestamp);	    	
	}
	
	private void readToCurrent(long clientTimestamp) {
		long serverTimestamp = clientTimestamp + interpolationTime;
		
		try {
			tryGetNewEntry(serverTimestamp);
		} catch (SQLException e) {
			LOGGER.error("Could not read to current", e);
		}
	}
	
	private void tryGetNewEntry(long serverTimeMax) throws SQLException {
		ServerSampleEntry newest = buffer.peek();
		
		long newestTimeInBuffer = newest != null ? newest.timestamp : serverTimeMax - interpolationTime;
	    
	    // Get all entries between the newest value in the buffer and the end of the interpolation timeframe
	    long firstTimestamp = newestTimeInBuffer / 1000 + 1; // +1, otherwise results include sample we already have
	    long lastTimestamp = serverTimeMax / 1000;
	    String query = "select * from bitstamp_ticker WHERE `timestamp` BETWEEN " + firstTimestamp + " AND " + lastTimestamp + " ORDER BY timestamp ASC";
	    ResultSet resultSet = connection.executeQueryWithRetries(query, MAXRETRIES);
	    
	    // Add them all to the buffer
	    while(resultSet.next()) {
	    	long serverTimestamp = resultSet.getLong("timestamp") * 1000;
	    	ServerSampleEntry entry = new ServerSampleEntry(serverTimestamp, 6);
	    	
	    	entry.set(SignalName.LAST.ordinal(), new Sample(serverTimestamp, resultSet.getDouble("last")));
			entry.set(SignalName.HIGH.ordinal(), new Sample(serverTimestamp, resultSet.getDouble("high")));
			entry.set(SignalName.LOW.ordinal(), new Sample(serverTimestamp, resultSet.getDouble("low")));
			entry.set(SignalName.VOLUME.ordinal(), new Sample(serverTimestamp, resultSet.getDouble("volume")));
			entry.set(SignalName.BID.ordinal(), new Sample(serverTimestamp, resultSet.getDouble("bid")));
			entry.set(SignalName.ASK.ordinal(), new Sample(serverTimestamp, resultSet.getDouble("ask")));
	    	
			LOGGER.debug("Query Result: " + entry);
			
			// TODO: this check should not be necessary anymore (see +1 trick above), but I'm paranoid
			ServerSampleEntry lastEntry = buffer.size() > 0 ? buffer.get(buffer.size()-1) : null; 
			if (!entry.equals(lastEntry))
				buffer.add(entry);
	    }
	    
	    try {
	    	resultSet.close();
	    } catch (SQLException e) {
	    	LOGGER.error("Failed to dispose properly of parsed ResultSet", e);
	    }
	}
	
	@Override
	public boolean isEmpty() {
		return false;
	}

	@Override
	public boolean open() {
		return connection.open();
	}

	@Override
	public boolean close() {
		return connection.close();
	}
}
