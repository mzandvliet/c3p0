package c3po;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.sql.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import c3po.BitstampTickerSource.ServerSampleEntry;

/**
 * Work in progress in using the database as source
 */
public class BitstampTickerDbSource extends BitstampTickerSource {
	private static final Logger LOGGER = LoggerFactory.getLogger(BitstampTickerDbSource.class);

	private Connection connect = null;
	private Statement statement = null;
	
	public BitstampTickerDbSource(long interpolationTime, InetSocketAddress host, String user, String pwd) throws ClassNotFoundException, SQLException {
		  super(interpolationTime);
		  
		  // This will load the MySQL driver, each DB has its own driver
	      Class.forName("com.mysql.jdbc.Driver");
	      // Setup the connection with the DB
	      connect = DriverManager.getConnection("jdbc:mysql://"+host.getHostString()+":"+host.getPort()+"/c3po?user="+user+"&password="+pwd);
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void tryGetNewEntry(long serverTimeMax) throws SQLException {
		ServerSampleEntry newest = buffer.peek();
		
		long newestTimeInBuffer = newest != null ? newest.timestamp : serverTimeMax - interpolationTime;
		 
	    // Statements allow to issue SQL queries to the database
	    statement = connect.createStatement();
	      
	    // Get all entries between the newest value in the buffer and the end of the interpolation timeframe
	    String query = "select * from bitstamp_ticker WHERE `timestamp` BETWEEN " + newestTimeInBuffer / 1000  + " AND " + serverTimeMax / 1000 + " ORDER BY timestamp ASC";
	    ResultSet resultSet = statement.executeQuery(query);
	    
	    // Add them all to the buffer
	    while(resultSet.next()) {
	    	long serverTimestamp = resultSet.getLong("timestamp") * 1000;
	    	ServerSampleEntry entry = new ServerSampleEntry(serverTimestamp, 6);
	    	entry.set(0, new Sample(serverTimestamp, resultSet.getDouble("ask")));
	    	entry.set(1, new Sample(serverTimestamp, resultSet.getDouble("last")));
	    	entry.set(2, new Sample(serverTimestamp, resultSet.getDouble("low")));
	    	entry.set(3, new Sample(serverTimestamp, resultSet.getDouble("high")));
	    	entry.set(4, new Sample(serverTimestamp, resultSet.getDouble("bid")));
	    	entry.set(5, new Sample(serverTimestamp, resultSet.getDouble("volume")));
	    	
	    	buffer.add(entry);
	    }
	}

	@Override
	public void open() {
		// Already opened in the constructor, might not be pretty though
	}

	@Override
	public void close() {
		try {
			connect.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean isEmpty() {
		return false;
	}
}
