package c3po.db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import c3po.DbConnection;
import c3po.IBot;
import c3po.ITickable;
import c3po.utils.Time;
import c3po.wallet.IWalletUpdateListener;
import c3po.wallet.WalletUpdateResult;

/**
 * Checks whether or not the bot is allowed to run. For that end he uses
 * the botId and apiKey combination.
 */
public class DbLicenceChecker implements ITickable {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(DbLicenceChecker.class);
	private static final int MAXRETRIES = 3;
	private static final long LICENCE_CHECK_TIMEOUT = 10 * Time.MINUTES;
	private static final long LICENCE_RETRY_INTERVAL = 1 * Time.MINUTES;

	private final IBot bot;
	private final String apiKey;
	private final DbConnection connection;
	
	private long lastCheck = 0;
	private boolean isLicenced = false;
	
	public DbLicenceChecker(IBot bot, String apiKey, DbConnection connection) {
		this.bot = bot;
		this.apiKey = apiKey;
		this.connection = connection;
	}
	
	protected boolean checkLicence() {
		try {
			// Count the amount of active licences for this botId/apiKey combination
			final String sqlTemplate = "SELECT count(*) FROM  `c3po`.`bots` WHERE bot_id = '%s' AND api_key = '%s' AND licenced = 1;";
			String sql = String.format(sqlTemplate, bot.getId(), apiKey);
			ResultSet result = connection.executeQueryWithRetries(sql, MAXRETRIES);
			this.lastCheck = new Date().getTime();

			if (result.getInt(0) == 1) {
				return true;
			} else {
				LOGGER.info("No active licence found for this bot");
				return false;
			}
		} catch (SQLException e) {
			LOGGER.info("Error while trying to fetch licence", e);
			return false;
		}
	}

	/**
	 * Method that checks if this bot still may trade
	 *  
	 * @return True if the bot is licenced, false if it must stop
	 */
	protected boolean isLicenced() {
		// If the last time is long ago, check if the licence is valid
		if(lastCheck + LICENCE_CHECK_TIMEOUT > new Date().getTime()) {
			this.isLicenced = this.checkLicence();
		}
		
		return isLicenced;
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
		while(isLicenced() == false) {
			try {
				Thread.sleep(LICENCE_RETRY_INTERVAL);
			} catch (InterruptedException e) {
				throw new RuntimeException("Error while awaiting the licencing process" + e);
			}
		}
	}
}
