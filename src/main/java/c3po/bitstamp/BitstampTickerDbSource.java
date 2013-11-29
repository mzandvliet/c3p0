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

	private Connection connect = null;
	private Statement statement = null;
	
	public BitstampTickerDbSource(long timestep, long interpolationTime, InetSocketAddress host, String user, String pwd) throws ClassNotFoundException, SQLException {
		  super(timestep, interpolationTime);
		  
		  // This will load the MySQL driver, each DB has its own driver
	      Class.forName("com.mysql.jdbc.Driver");
	      // Setup the connection with the DB
	      connect = DriverManager.getConnection("jdbc:mysql://"+host.getHostName()+":"+host.getPort()+"/c3po?user="+user+"&password="+pwd);
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
	    	
	    	entry.set(SignalName.LAST.ordinal(), new Sample(serverTimestamp, resultSet.getDouble("last")));
			entry.set(SignalName.HIGH.ordinal(), new Sample(serverTimestamp, resultSet.getDouble("high")));
			entry.set(SignalName.LOW.ordinal(), new Sample(serverTimestamp, resultSet.getDouble("low")));
			entry.set(SignalName.VOLUME.ordinal(), new Sample(serverTimestamp, resultSet.getDouble("volume")));
			entry.set(SignalName.BID.ordinal(), new Sample(serverTimestamp, resultSet.getDouble("bid")));
			entry.set(SignalName.ASK.ordinal(), new Sample(serverTimestamp, resultSet.getDouble("ask")));
	    	
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
