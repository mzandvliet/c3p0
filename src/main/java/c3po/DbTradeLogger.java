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
 * TODO: 
 * - share code with the BitstampTickerDbSource
 * - this turns exception flow into program flow, is that best practice? I imagine users of this class don't necessarily have to know about SQL implementation details, at least. 
 */
public class DbTradeLogger implements ITradeListener, IWalletTransactionListener {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(DbTradeLogger.class);
	private static final int MAXRETRIES = 3;

	private final IBot bot;
	private final InetSocketAddress host;
	private final String user;
	private final String pwd;
	
	private Connection connection = null;
	
	public DbTradeLogger(IBot bot, InetSocketAddress host, String user, String pwd) {
		this.bot = bot;
		this.host = host;
		this.user = user;
		this.pwd = pwd;
		
		bot.addTradeListener(this);
		bot.getWallet().addListener(this);
		
		// This will load the MySQL driver, each DB has its own driver
	    try {
			Class.forName("com.mysql.jdbc.Driver");
		} catch (ClassNotFoundException e) {
			LOGGER.error("Could not load database driver", e);
		}
	}

	public boolean open() {
		// Setup the connection with the DB
		try {
			connection = DriverManager.getConnection("jdbc:mysql://"+host.getHostName()+":"+host.getPort()+"/c3po?user="+user+"&password="+pwd);
			return true;
		} catch (SQLException e) {
			LOGGER.error("Could not open connection", e);
			return false;
		}
	}
	
	public boolean close() {
		// Close the connection with the DB
		try {
			connection.close();
			return true;
		} catch (SQLException e) {
			LOGGER.error("Could not close connection", e);
			return false;
		}
	}
	
	protected void reconnect() {
		LOGGER.debug("Attempting to reconnect...");
		close(); // First, attempt to clean up the old connection to avoid any leaks
		open(); // Then, open up a new connection
	}

	public void startSession(long startTime) {
		// Send botID to database
		final String sqlTemplate = "INSERT INTO  `c3po`.`bots` (`bot_id` ,`bot_type` ,`tradefloor_type` ,`start`) VALUES ('%s',  '%s',  '%s',  '%s')";
		String sql = String.format(sqlTemplate, bot.getId(), "MacdBot", "BitstampSimulationTradeFloor", startTime / 1000);
		executeStatementWithRetries(sql, MAXRETRIES);
		
		// Initialize wallet log for this session
		onWalletTransaction(new WalletTransactionResult(startTime, bot.getWallet().getWalletUsd(), bot.getWallet().getWalletBtc()));
	}
	
	@Override
	public void onTrade(TradeAction action) {
		final String sqlTemplate = "INSERT INTO  `c3po`.`bot_trade_action` (`bot_id` ,`timestamp` ,`action_type` ,`amount`) VALUES ('%s',  '%s',  '%s',  '%s')";
		String sql = String.format(sqlTemplate, bot.getId(), Math.floor(action.timestamp / 1000.0d), action.action, action.volume);
		executeStatementWithRetries(sql, MAXRETRIES);
	}
	
	@Override
	public void onWalletTransaction(WalletTransactionResult transaction) {
		final String sqlTemplate = "INSERT INTO  `c3po`.`bot_wallet` (`bot_id` ,`timestamp` ,`walletUsd` ,`walletBtc`) VALUES ('%s',  '%s',  '%s',  '%s')";
		String sql = String.format(sqlTemplate, bot.getId(), transaction.timestamp / 1000, transaction.usdTotal, transaction.btcTotal);
		executeStatementWithRetries(sql, MAXRETRIES);
	}

	private boolean executeStatementWithRetries(String sql, int maxRetries) {
		int i = 0;
		
		while (i < maxRetries) {
			if (executeStatement(sql)) {
				LOGGER.debug(String.format("Succesfully executed statement after %s tries.", i + 1));
				return true;
			} else {
				reconnect();
			}
			i++;
		}
		
		LOGGER.error(String.format("Failed to execute statement after %s tries.", maxRetries + 1));
		return false;
	}
	
	private boolean executeStatement(String sql) {
		Statement statement = null;
		try {
			LOGGER.debug("executing: " + sql);
			statement = connection.createStatement();
			statement.execute(sql);
			return true;
		} catch (SQLException e) {
			LOGGER.error("Failed to execute statement", e);
			return false;
		}			
	}
}
