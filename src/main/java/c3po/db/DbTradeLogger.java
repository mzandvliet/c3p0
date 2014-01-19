package c3po.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import c3po.IBot;
import c3po.ITradeListener;
import c3po.structs.TradeIntention;
import c3po.wallet.IWalletUpdateListener;
import c3po.wallet.WalletUpdateResult;

/**
 * This class listens on Trade and Wallet updates and logs them in the bot specific
 * database.
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

	@Override
	public void onTrade(TradeIntention action) {
		LOGGER.debug("Logging " + action);
		final String sqlTemplate = "INSERT INTO  `%s`.`bot_trade_action` (`timestamp` ,`action_type` ,`amount`) VALUES ('%s',  '%s',  '%s',  '%s')";
		String sql = String.format(sqlTemplate, "bot_"+bot.getId(), Math.floor(action.timestamp / 1000.0d), action.action, action.volume);
		connection.executeStatementWithRetries(sql, MAXRETRIES);
	}
	
	@Override
	public void onWalletUpdate(WalletUpdateResult transaction) {
		LOGGER.debug("Logging " + transaction);
		final String sqlTemplate = "INSERT INTO  `%s`.`bot_wallet` (`timestamp` ,`walletUsd` ,`walletBtc`) VALUES ('%s',  '%s',  '%s',  '%s')";
		String sql = String.format(sqlTemplate, "bot_"+bot.getId(), transaction.timestamp / 1000, transaction.usdTotal, transaction.btcTotal);
		connection.executeStatementWithRetries(sql, MAXRETRIES);
	}
}
