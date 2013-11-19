package c3po;

import java.net.InetSocketAddress;
import java.sql.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Work in progress in using the database as source
 */
public class DbTradeLogger implements ITradeListener {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(DbTradeLogger.class);

	private IBot bot;
	private long botHash;
	
	private Connection connect = null;
	private Statement statement = null;
	
	public DbTradeLogger(IBot bot, InetSocketAddress host, String user, String pwd) {
		this.bot = bot;
		this.botHash = bot.hashCode();
		bot.addListener(this);
	}

	@Override
	public void onTrade(TradeAction action) {
		
	}
	
	private void startSession() throws SQLException {
		// Send botID to database
		
		long fetchTime = 0;
		  
	      // Statements allow to issue SQL queries to the database
	      statement = connect.createStatement();
	      // Result set get the result of the SQL query
	      final String queryTemplate = "INSERT INTO  `c3po`.`bots` (`bot_id` ,`bot_type` ,`tradefloor_type` ,`start`) VALUES ('%s',  '%s',  '%s',  '%s')";
	      String query = String.format(null, botHash, "MacdBot", "BitstampSimulationTradeFloor", 0);
	      
	      ResultSet resultSet = statement.executeQuery(queryTemplate);
	      
	      LOGGER.debug(queryTemplate);
	}
	
	private void log(TradeAction action) throws SQLException {
		// Send botID + action to database
		
		long fetchTime = 0;
		  
	      // Statements allow to issue SQL queries to the database
	      statement = connect.createStatement();
	      // Result set get the result of the SQL query
	      String query = "select * from bitstamp_ticker WHERE `timestamp` >= " + fetchTime  + " ORDER BY timestamp ASC";
	      ResultSet resultSet = statement.executeQuery(query);
	      
	      LOGGER.debug(query);
	      
	      while(resultSet.next()) {

	      }
	}
	
	public void open(InetSocketAddress host, String user, String pwd) throws ClassNotFoundException, SQLException {
		// This will load the MySQL driver, each DB has its own driver
	      Class.forName("com.mysql.jdbc.Driver");
	      // Setup the connection with the DB
	      connect = DriverManager.getConnection("jdbc:mysql://"+host.getHostString()+":"+host.getPort()+"/c3po?user="+user+"&password="+pwd);
	}

	public void close() {
		try {
			connect.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public boolean isEmpty() {
		return false;
	}
}
