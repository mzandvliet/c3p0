package c3po;

import java.net.InetSocketAddress;
import java.sql.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Work in progress in using the database as source
 */
public class BitstampTickerDbSource extends BitstampTickerSource {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(BitstampTickerDbSource.class);

	private Connection connect = null;
	private Statement statement = null;
	ArrayList<BitstampTickerRow> buffer;

	
	public BitstampTickerDbSource(InetSocketAddress host, String user, String pwd) throws ClassNotFoundException, SQLException {
		  super();
		  
		  // This will load the MySQL driver, each DB has its own driver
	      Class.forName("com.mysql.jdbc.Driver");
	      // Setup the connection with the DB
	      connect = DriverManager.getConnection("jdbc:mysql://"+host.getHostString()+":"+host.getPort()+"/c3po?user="+user+"&password="+pwd);
	}
	
	@Override
	public int getNumOutputs() {
		return signals.length;
	}
	
	@Override
	public ISignal getOutput(int i) {
		return signals[i];
	}
	
	private void fetchData(long timestamp) throws SQLException {
		
		  long fetchTime = (timestamp / 1000) - 60;
		  
	      // Statements allow to issue SQL queries to the database
	      statement = connect.createStatement();
	      // Result set get the result of the SQL query
	      String query = "select * from bitstamp_ticker WHERE `timestamp` >= " + fetchTime  + " ORDER BY timestamp ASC";
	      ResultSet resultSet = statement.executeQuery(query);
	      LOGGER.debug(query);
	      
	      // I dont understand shit of this resultSet class so I just use my own buffer
	      buffer = new ArrayList<BitstampTickerRow>();
	      while(resultSet.next()) {
	    	  buffer.add(new BitstampTickerRow(
	    			  resultSet.getLong("timestamp"), 
	    			  resultSet.getDouble("ask"), 
	    			  resultSet.getDouble("last"), 
	    			  resultSet.getDouble("low"), 
	    			  resultSet.getDouble("high"), 
	    			  resultSet.getDouble("bid"), 
	    			  resultSet.getLong("volume")));
	      }
	}
	
	@Override
	public long getLastTick() {
		return lastTick;
	}

	@Override
	public void tick(long tick) {
		if (tick > lastTick) {
			query(tick);
		}
		lastTick = tick;
	}
	
	public void query(long tick) {
		try {
			// The first time, fill the buffer
			do {
			  fetchData(tick); 
			  Thread.sleep(1000);
			} while(buffer.size() < 1);
			
			BitstampTickerRow prev = buffer.get(0);
			
			if(buffer.size() == 1) {
				useRow(prev);
			    return;
			}
			
			BitstampTickerRow current = null;
			for(int index = 1; index < buffer.size(); index++) {
				current = buffer.get(index);
				
				// If the next is too new, use the prev
				if(current.timestamp > tick) {
					useRow(prev);
					return;
				}
			}
			
			useRow(current);
			return;
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} 
	}
	
	private void useRow(BitstampTickerRow row) throws SQLException {
		
		LOGGER.debug("Using row " + row);
		signals[SignalName.ASK.ordinal()].setSample(new Sample(row.timestamp, row.ask));
		signals[SignalName.BID.ordinal()].setSample(new Sample(row.timestamp, row.bid));
		signals[SignalName.HIGH.ordinal()].setSample(new Sample(row.timestamp, row.high));
		signals[SignalName.LAST.ordinal()].setSample(new Sample(row.timestamp, row.last));
		signals[SignalName.LOW.ordinal()].setSample(new Sample(row.timestamp, row.low));
		signals[SignalName.VOLUME.ordinal()].setSample(new Sample(row.timestamp, row.volume));
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
	
	/**
	 * Quick struct to store a record
	 * from the ticker
	 */
	public class BitstampTickerRow {
		public long timestamp;
		public double ask;
		public double last;
		public double low;
		public double high;
		public double bid;
		public double volume;
		
		public BitstampTickerRow(long timestamp, double ask, double last,
				double low, double high, double bid, double volume) {
			this.timestamp = timestamp;
			this.ask = ask;
			this.last = last;
			this.low = low;
			this.high = high;
			this.bid = bid;
			this.volume = volume;
		}
	}
}
