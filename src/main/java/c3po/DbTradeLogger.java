package c3po;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import c3po.wallet.IWalletUpdateListener;
import c3po.wallet.WalletUpdateResult;

/**
 * Work in progress in using the database as source
 */
public class DbTradeLogger implements ITradeListener, IWalletUpdateListener {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(DbTradeLogger.class);
	private static final int MAXRETRIES = 3;

	private final IBot bot;
	private DbConnection connection = null;
	
	public DbTradeLogger(IBot bot, DbConnection connection) {
		this.bot = bot;
		this.connection = connection;
		
		bot.addTradeListener(this);
		bot.getWallet().addListener(this);
	}
	
	public void startSession(long startTime) {
		// Send botID to database
		final String sqlTemplate = "INSERT INTO  `c3po`.`bots` (`bot_id` ,`bot_type` ,`tradefloor_type` ,`start`) VALUES ('%s',  '%s',  '%s',  '%s')";
		String sql = String.format(sqlTemplate, bot.getId(), "MacdBot", "BitstampSimulationTradeFloor", startTime / 1000);
		connection.executeStatementWithRetries(sql, MAXRETRIES);
	}
	
	@Override
	public void onTrade(TradeAction action) {
		final String sqlTemplate = "INSERT INTO  `c3po`.`bot_trade_action` (`bot_id` ,`timestamp` ,`action_type` ,`amount`) VALUES ('%s',  '%s',  '%s',  '%s')";
		String sql = String.format(sqlTemplate, bot.getId(), Math.floor(action.timestamp / 1000.0d), action.action, action.volume);
		connection.executeStatementWithRetries(sql, MAXRETRIES);
	}
	
	@Override
	public void onWalletUpdate(WalletUpdateResult transaction) {
		final String sqlTemplate = "INSERT INTO  `c3po`.`bot_wallet` (`bot_id` ,`timestamp` ,`walletUsd` ,`walletBtc`) VALUES ('%s',  '%s',  '%s',  '%s')";
		String sql = String.format(sqlTemplate, bot.getId(), transaction.timestamp / 1000, transaction.usdTotal, transaction.btcTotal);
		connection.executeStatementWithRetries(sql, MAXRETRIES);
	}
}
