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
	
	/**
	 * The amount of time the licence is not checked after being validated
	 */
	private static final long LICENCE_CHECK_TIMEOUT = 10 * Time.MINUTES;
	
	/**
	 * Retry timer if licencing fails
	 */
	private static final long LICENCE_RETRY_INTERVAL = 1 * Time.MINUTES;

	private final IBot bot;
	private final String apiKey;
	private final DbConnection connection;
	
	/**
	 * The last time the licence was approved
	 */
	private long lastCheckSuccess = 0;
	
	/**
	 * Last fetched licence status
	 */
	private boolean isLicenced = false;
	
	public DbLicenceChecker(IBot bot, String apiKey, DbConnection connection) {
		this.bot = bot;
		this.apiKey = apiKey;
		this.connection = connection;
	}
	
	/**
	 * DB Query to check if the licence is valid
	 * @return
	 */
	protected boolean checkLicence() {
		try {
			// Count the amount of active licences for this botId/apiKey combination
			final String sqlTemplate = "SELECT count(*) FROM  `c3po`.`bots` WHERE `id` = '%s' AND `api_key` = '%s' AND `licenced` = 1;";
			String sql = String.format(sqlTemplate, bot.getId(), apiKey);
			ResultSet result = connection.executeQueryWithRetries(sql, MAXRETRIES);

			if (result.next() && result.getInt(1) == 1) {
				this.lastCheckSuccess = new Date().getTime();
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
		if(lastCheckSuccess + LICENCE_CHECK_TIMEOUT < new Date().getTime()) {
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
