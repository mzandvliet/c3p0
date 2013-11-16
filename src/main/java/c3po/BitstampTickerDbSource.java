package c3po;

import java.net.InetSocketAddress;
import java.sql.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Work in progress in using the database as source
 */
public class BitstampTickerDbSource extends BitstampTickerSource {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(BitstampTickerDbSource.class);

	private Connection connect = null;
	private Statement statement = null;
	private ResultSet resultSet = null;

	
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
	      // Statements allow to issue SQL queries to the database
	      statement = connect.createStatement();
	      // Result set get the result of the SQL query
	      String query = "select * from bitstamp_ticker WHERE timestamp < " + timestamp + " ORDER BY timestamp DESC LIMIT 1";
	      resultSet = statement.executeQuery(query);
	      LOGGER.debug(query);
	      resultSet.next();
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
			fetchData(tick); 
			useCurrentRow();
		} catch (SQLException e) {
			e.printStackTrace();
		} 
	}
	
	private void useCurrentRow() throws SQLException {
		signals[SignalName.ASK.ordinal()].setSample(new Sample(resultSet.getLong("timestamp"), resultSet.getDouble("ask")));
		signals[SignalName.BID.ordinal()].setSample(new Sample(resultSet.getLong("timestamp"), resultSet.getDouble("bid")));
		signals[SignalName.HIGH.ordinal()].setSample(new Sample(resultSet.getLong("timestamp"), resultSet.getDouble("high")));
		signals[SignalName.LAST.ordinal()].setSample(new Sample(resultSet.getLong("timestamp"), resultSet.getDouble("last")));
		signals[SignalName.LOW.ordinal()].setSample(new Sample(resultSet.getLong("timestamp"), resultSet.getDouble("low")));
		signals[SignalName.VOLUME.ordinal()].setSample(new Sample(resultSet.getLong("timestamp"), resultSet.getDouble("volume")));
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
		public double buy;
		public double sell;
		public double high;
		public double bid;
		public double volume;
		
		public BitstampTickerRow(long timestamp, double ask, double buy,
				double sell, double high, double bid, double volume) {
			this.timestamp = timestamp;
			this.ask = ask;
			this.buy = buy;
			this.sell = sell;
			this.high = high;
			this.bid = bid;
			this.volume = volume;
		}
	}
}
