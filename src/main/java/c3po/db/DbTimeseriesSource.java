package c3po.db;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import c3po.*;
import c3po.utils.SignalMath;

/**
 * Work in progress in using the database as source.
 * 
 * Different usages:
 * - Simulation, start-end time
 * - Warmup (same as simulation?)
 * - Ongoing
 */
public class DbTimeseriesSource {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(DbTimeseriesSource.class);
	private static final int MAXRETRIES = 3;

	private DbConnection connection = null;
	
	private String tableName;
	private List<String> columns;
	
	private List<ServerSnapshot> data = new ArrayList<ServerSnapshot>();
	private int lastDataIndex = 0;
	
	public DbTimeseriesSource(long timestep, DbConnection connection, String tableName, List<String> columns) {
		  this.connection = connection;
		  this.tableName = tableName;
		  this.columns = columns;
	}
	
	/**
	 * Returns a list of ServerSampleEntries that are newer than the newest returned Result
	 * and the maxTimestamp.
	 * 
	 * @param maxTimestamp
	 * @return
	 */
	public List<ServerSnapshot> getNewSamplesUntil(long maxTimestamp) {
		List<ServerSnapshot> results = new ArrayList<ServerSnapshot>();

		// All the querying is done ... now look in the buffer what we want to return
		long lastTimestamp = 0;
		while(lastTimestamp < maxTimestamp && lastDataIndex < data.size()) {
			ServerSnapshot serverSnapshot = data.get(lastDataIndex);
			
			for (Sample sample : serverSnapshot.samples) {
				sample.toString();
			}
			
			results.add(serverSnapshot);
			lastTimestamp = serverSnapshot.timestamp;
			lastDataIndex++;
		}
		
		return results;
	}

	/**
	 * This method fetches data and adds it to it's internal buffer.
	 * Use {@link getNewSamples(long)} to get the Results.
	 * 
	 * @param start
	 * @param end
	 */
	public void fetchDataFromDatabase(long start, long end) {
	    long firstTimestamp = start / 1000 + 1; // +1, otherwise results include sample we already have
	    long lastTimestamp = end / 1000;
	    String query = "select * from "+ tableName + " WHERE `timestamp` BETWEEN " + firstTimestamp + " AND " + lastTimestamp + " ORDER BY timestamp ASC";
	    ResultSet resultSet = connection.executeQueryWithRetries(query, MAXRETRIES);
	    try {
		    // Add them all to the buffer
		    while(resultSet.next()) {
		    	long sampleTimestamp = resultSet.getLong("timestamp") * 1000;
		    	ServerSnapshot entry = new ServerSnapshot(sampleTimestamp, columns.size());
		    	
		    	for(String column : columns) {
		    		double value = resultSet.getDouble(column);
		    		
		    		if (!SignalMath.isValidNumber(value) || value == 0d)
		    			throw new IllegalStateException("Received illegal sample from database. Timestamp: " + sampleTimestamp + ", Column: " + column + ", Value: " + value);
		    		
		    		entry.set(getSignalIndexByName(column), new Sample(sampleTimestamp, value));
		    	}

		    	data.add(entry);
		    }
	    
	    	resultSet.close();
	    } catch (SQLException e) {
	    	LOGGER.error("Failed to dispose properly of parsed ResultSet", e);
	    }
	}
	
	private int getSignalIndexByName(String name) {
		for(int i = 0; i < columns.size(); i++) {
			if(columns.get(i).equals(name)) {
				return i;
			}
		}
		
		throw new RuntimeException("Could not find index for " + name);
	}
	
	public void reset() {
		lastDataIndex = 0;
	}
	
	public void resetToTimestamp(long startTimestamp) {
		for (int i = 0; i < data.size(); i++) {
			if (data.get(i).timestamp > startTimestamp) {
				lastDataIndex = Math.max(i-1, 0);
				return;
			}
		}
	}

	public boolean open() {
		return connection.open();
	}

	public boolean close() {
		return connection.close();
	}
}
