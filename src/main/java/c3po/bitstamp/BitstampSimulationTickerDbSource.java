package c3po.bitstamp;

import java.net.InetSocketAddress;
import java.sql.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import c3po.*;

/**
 * Work in progress in using the database as source
 */
public class BitstampSimulationTickerDbSource extends BitstampTickerSource implements INonRealtimeSource {
	private static final Logger LOGGER = LoggerFactory.getLogger(BitstampSimulationTickerDbSource.class);
	
	private final long startTime;
	private final long endTime;
	private Connection connection = null;
	private ResultSet resultSet;
	
	public BitstampSimulationTickerDbSource(long timestep, long interpolationTime, InetSocketAddress host, String user, String pwd, long startTime, long endTime) {
		  super(timestep, interpolationTime);
		  
		  this.startTime = startTime;
		  this.endTime = endTime;
		  
	    try {
	    	  // This will load the MySQL driver, each DB has its own driver
			Class.forName("com.mysql.jdbc.Driver");
			// Setup the connection with the DB
		      connection = DriverManager.getConnection("jdbc:mysql://"+host.getHostString()+":"+host.getPort()+"/c3po?user="+user+"&password="+pwd);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
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
		
	    // Add them all to the buffer
	    while(newestTimeInBuffer < serverTimeMax && resultSet.next()) {
	    	newestTimeInBuffer = resultSet.getLong("timestamp") * 1000;
	    	ServerSampleEntry entry = new ServerSampleEntry(newestTimeInBuffer, 6);
	    	
	    	entry.set(SignalName.LAST.ordinal(), new Sample(newestTimeInBuffer, resultSet.getDouble("last")));
			entry.set(SignalName.HIGH.ordinal(), new Sample(newestTimeInBuffer, resultSet.getDouble("high")));
			entry.set(SignalName.LOW.ordinal(), new Sample(newestTimeInBuffer, resultSet.getDouble("low")));
			entry.set(SignalName.VOLUME.ordinal(), new Sample(newestTimeInBuffer, resultSet.getDouble("volume")));
			entry.set(SignalName.BID.ordinal(), new Sample(newestTimeInBuffer, resultSet.getDouble("bid")));
			entry.set(SignalName.ASK.ordinal(), new Sample(newestTimeInBuffer, resultSet.getDouble("ask")));
	    	
	    	buffer.add(entry);
	    }
	}

	@Override
	public void open() {		
		try {
			// Statements allow to issue SQL queries to the database
			Statement statement = connection.createStatement();
			// Get all entries from the simulation start time to the simulation end time
		    String query = "select * from bitstamp_ticker WHERE `timestamp` BETWEEN " + startTime / 1000  + " AND " + endTime / 1000 + " ORDER BY timestamp ASC";
		    resultSet = statement.executeQuery(query);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return;
	}

	@Override
	public void close() {
		try {
			connection.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
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
		if (resultSet != null) {
			try {
				resultSet.first();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		buffer.clear();
		
		for (OutputSignal signal : signals) {
			signal.setSample(Sample.none);
		}
	}
}
