package c3po.db;

import java.sql.*;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import c3po.*;

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
	
	private LinkedList<ServerSnapshot> data = new LinkedList<ServerSnapshot>();
	private int lastDataIndex = 0;
	
	/**
	 * Timestamp of the latest sample that is fetched from the source
	 */
	private long latestSampleTimestamp;
	
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
	public List<ServerSnapshot> getNewSamples(long maxTimestamp) {
		List<ServerSnapshot> results = new LinkedList<ServerSnapshot>();
		// If we would like data newer then the newest sample that returned from a query
		if(maxTimestamp > latestSampleTimestamp) {
			// Query for possible new data
			fetchDataFromDatabase(latestSampleTimestamp, maxTimestamp);
		}
		
		// All the querying is done ... now look in the buffer what we want to return 
		while(true) {
			// No more data elements
			if(data.size() <= lastDataIndex)
				break;
			
			ServerSnapshot sample = data.get(lastDataIndex);
			
			// This sample is OK, add it to the return list and move the marker
			if(sample.timestamp < maxTimestamp) {
				results.add(sample);
				lastDataIndex++;
			} 
			// Current result is too new for the request, stop the call
			else {
				break;
			}
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
		    		entry.set(getSignalIndexByName(column), new Sample(sampleTimestamp, resultSet.getDouble(column)));
		    	}
		    	
		    	if(sampleTimestamp > this.latestSampleTimestamp) {
		    		this.latestSampleTimestamp = sampleTimestamp;
		    	}
		    	
		    	data.add(entry);
		    }
	    
	    	resultSet.close();
	    } catch (SQLException e) {
	    	LOGGER.error("Failed to dispose properly of parsed ResultSet", e);
	    }
	}
	
	/**
	 * Manual setting of latestSampleTimestamp. Can be useful
	 * when there is no preloading.
	 * 
	 * @param latestSampleTimestamp
	 */
	public void setLatestSampleTimestamp(long latestSampleTimestamp) {
		this.latestSampleTimestamp = latestSampleTimestamp;
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

	public boolean open() {
		return connection.open();
	}

	public boolean close() {
		return connection.close();
	}
}
