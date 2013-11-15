package c3po;

import java.net.InetSocketAddress;
import java.sql.*;

/**
 * Work in progress in using the database as source
 */
public class BitstampTickerDbSource implements ICompositeSignal {
	private SurrogateSignal[] signals;
	private Connection connect = null;
	  private Statement statement = null;
	  private ResultSet resultSet = null;
	
	public BitstampTickerDbSource(InetSocketAddress host, String user, String pwd) throws ClassNotFoundException, SQLException {
		 // This will load the MySQL driver, each DB has its own driver
	      Class.forName("com.mysql.jdbc.Driver");
	      // Setup the connection with the DB
	      connect = DriverManager.getConnection("jdbc:mysql://"+host.getHostString()+":"+host.getPort()+"/c3po?user="+user+"&password="+pwd);

	      // Statements allow to issue SQL queries to the database
	      statement = connect.createStatement();
	      // Result set get the result of the SQL query
	      resultSet = statement.executeQuery("select * from bitstamp_ticker");
	      
	      // ResultSet is initially before the first data set
	      int i = 0;
	      while (resultSet.next()) {
	    	  signals[i].setSample(new Sample(resultSet.getLong("timestamp"), resultSet.getDouble("high")));
	      }
	}
	
	@Override
	public int getNumSignals() {
		return signals.length;
	}
	
	@Override
	public ISignal get(int i) {
		return signals[i];
	}
	
	public enum SignalName {
		LAST,
	    HIGH,
	    LOW,
	    VOLUME,
	    BID,
	    ASK
	}

	@Override
	public void tick(long tick) {
		// TODO Auto-generated method stub
		
	}
}
