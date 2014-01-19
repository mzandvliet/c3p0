package c3po.db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import c3po.IBot;
import c3po.ITickable;
import c3po.ITradeListener;
import c3po.structs.TradeIntention;
import c3po.wallet.IWalletUpdateListener;
import c3po.wallet.WalletUpdateResult;

/**
 * This classes manages the session of a running bot.
 * It instantiates a session on startup and updates
 * the end timestamp when needed.
 */
public class DbBotSession implements ITickable {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(DbBotSession.class);
	private static final int MAXRETRIES = 3;

	private final IBot bot;
	private DbConnection connection = null;
	private int sessionId;
	
	public DbBotSession(IBot bot, DbConnection connection) {
		this.bot = bot;
		this.connection = connection;
	}
	
	/**
	 * This method registers the start of a bot session. It stores the current
	 * version and config so we can keep track of what's running what.
	 * 
	 * @param version Version ID of the Bot
	 * @throws SQLException When the session could not be created
	 */
	public void startSession(String version) throws SQLException {
		// Create a new session
		
		final String sqlTemplate = "INSERT INTO  `%s`.`bot_session` (`config`, `version`, `start_time`, `last_time`) VALUES ('%s', '%s',  UNIX_TIMESTAMP(), UNIX_TIMESTAMP())";
		String sql = String.format(sqlTemplate, "bot_"+bot.getId(), bot.getConfig().toEscapedJSON(), version);
		connection.executeStatementWithRetries(sql, MAXRETRIES);
		
		// Fetch the session ID
		ResultSet rs = connection.executeQueryWithRetries(String.format("select last_insert_id() as session_id from `%s`.`bot_session`", "bot_"+bot.getId()), MAXRETRIES);
		this.sessionId  = rs.getInt("session_id");
		
		LOGGER.info("Registering start of session #" + sessionId);
	}
	
	private void updateSession() {
		final String sqlTemplate = "UPDATE  `%s`.`bot_session` SET `last_time` = UNIX_TIMESTAMP() WHERE `session_id` = %s";
		String sql = String.format(sqlTemplate, "bot_"+bot.getId(), sessionId);
		connection.executeStatementWithRetries(sql, MAXRETRIES);
	}

	@Override
	public long getTimestep() {
		return 0;
	}

	@Override
	public long getLastTick() {
		return 0;
	}

	@Override
	public void tick(long tick) {
		updateSession();
	}
}
