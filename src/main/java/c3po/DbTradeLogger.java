package c3po;

import java.net.InetSocketAddress;
import java.sql.*;
import java.util.Date;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Work in progress in using the database as source
 * 
 * TODO Remove duplication, cleanup. Share code with the BitstampTickerDbSource
 */
public class DbTradeLogger implements ITradeListener, IWalletTransactionListener {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(DbTradeLogger.class);

	private final IBot bot;
	private final InetSocketAddress host;
	private final String user;
	private final String pwd;
	
	private Connection connect = null;
	
	/**
	 * Constructor that allows a predefined botId, for restarting existing bots.
	 * 
	 * @param bot
	 * @param botId
	 * @param host
	 * @param user
	 * @param pwd
	 */
	public DbTradeLogger(IBot bot, InetSocketAddress host, String user, String pwd) {
		this.bot = bot;
		this.host = host;
		this.user = user;
		this.pwd = pwd;
		
		bot.addTradeListener(this);
		bot.getWallet().addListener(this);
	}

	@Override
	public void onTrade(TradeAction action) {
		log(action, true);
	}
	
	@Override
	public void onWalletTransaction(WalletTransactionResult transaction) {
		log(transaction, true);
	}

	public void open() throws ClassNotFoundException, SQLException {
		// This will load the MySQL driver, each DB has its own driver
	      Class.forName("com.mysql.jdbc.Driver");
	      // Setup the connection with the DB
	      connect = DriverManager.getConnection("jdbc:mysql://"+host.getHostName()+":"+host.getPort()+"/c3po?user="+user+"&password="+pwd);
	}
	
	/**
	 * This method tries to reconnect
	 * TODO Test and remove duplication
	 */
	protected void reconnect() {
		 try {
			connect = DriverManager.getConnection("jdbc:mysql://"+host.getHostName()+":"+host.getPort()+"/c3po?user="+user+"&password="+pwd);
		} catch (SQLException e) {
			LOGGER.error("Could not reconnect", e);
		}
	}

	public void close() {
		try {
			connect.close();
		} catch (SQLException e) {
			LOGGER.info("Could not close connection", e);
		}
	}
	
	public void startSession(long startTime) throws SQLException {
		// Send botID to database
		  
		// Statements allow to issue SQL queries to the database
		Statement statement = connect.createStatement();
		// Result set get the result of the SQL query
		final String queryTemplate = "INSERT INTO  `c3po`.`bots` (`bot_id` ,`bot_type` ,`tradefloor_type` ,`start`) VALUES ('%s',  '%s',  '%s',  '%s')";
		String query = String.format(queryTemplate, bot.getId(), "MacdBot", "BitstampSimulationTradeFloor", startTime / 1000);
		statement.execute(query);
		  
		LOGGER.debug(query);
		log(new WalletTransactionResult(startTime, bot.getWallet().getWalletUsd(), bot.getWallet().getWalletBtc()), true);
	}
	
	private void log(TradeAction action, boolean retry) {
		try {
			// Send botID + action to database
			
			// Statements allow to issue SQL queries to the database
			Statement statement = connect.createStatement();
			// Result set get the result of the SQL query
			final String queryTemplate = "INSERT INTO  `c3po`.`bot_trade_action` (`bot_id` ,`timestamp` ,`action_type` ,`amount`) VALUES ('%s',  '%s',  '%s',  '%s')";
			String query = String.format(queryTemplate, bot.getId(), Math.floor(action.timestamp / 1000.0d), action.action, action.volume);
			
			LOGGER.debug(query);
			statement.execute(query);
		} catch(SQLException e) {
			LOGGER.error("Could not log trade action", e);
			if(retry) {
				// Try reconnect and do it again
				reconnect();
				log(action,false);
			} 
		}
	}
	
	// Todo: init call with a start amount
	// Send updates
	
	private void log(WalletTransactionResult transaction, boolean retry) {
		try {
			// Send botID + action to database
			
			// Statements allow to issue SQL queries to the database
			Statement statement = connect.createStatement();
			// Result set get the result of the SQL query
			final String queryTemplate = "INSERT INTO  `c3po`.`bot_wallet` (`bot_id` ,`timestamp` ,`walletUsd` ,`walletBtc`) VALUES ('%s',  '%s',  '%s',  '%s')";
			String query = String.format(queryTemplate, bot.getId(), transaction.timestamp / 1000, transaction.usdTotal, transaction.btcTotal);
			
			statement.execute(query);
			  
			LOGGER.debug(query);
		} catch(SQLException e) {
			LOGGER.error("Could not log wallet transaction", e);
			if(retry) {
				// Try reconnect and do it again
				reconnect();
				log(transaction,false);
			} 
		}
	}
}
