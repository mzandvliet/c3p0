package c3po.db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import c3po.IBot;
import c3po.IOverrideModusChecker;
import c3po.ITickable;
import c3po.utils.Time;
import c3po.wallet.IWalletUpdateListener;
import c3po.wallet.WalletUpdateResult;

/**
 * Periodically fetches the bot override modus. This can be set using
 * the web application and can alter the bot's decision model.
 */
public class DbOverrideModusChecker implements IOverrideModusChecker {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(DbOverrideModusChecker.class);
	private static final int MAXRETRIES = 3;
	
	/**
	 * Retry timer
	 */
	private static final long RETRY_INTERVAL = 1 * Time.MINUTES;

	private final IBot bot;
	private final String apiKey;
	private final DbConnection connection;
	
	/**
	 * The last fetched modus
	 */
	private OverrideModus modus = OverrideModus.NONE;
	private long lastFetchMoment = 0;
	private long lastTick;

	public DbOverrideModusChecker(IBot bot, String apiKey, DbConnection connection) {
		this.bot = bot;
		this.apiKey = apiKey;
		this.connection = connection;
	}
	
	/**
	 * DB Query to check if there is currently a modus override
	 * @return
	 */
	protected boolean checkOverrideModus() {
		try {
			// Count the amount of active licences for this botId/apiKey combination
			final String sqlTemplate = "SELECT `override_modus` FROM  `c3po`.`bots` WHERE `id` = '%s' AND `api_key` = '%s';";
			String sql = String.format(sqlTemplate, bot.getId(), apiKey);
			ResultSet result = connection.executeQueryWithRetries(sql, MAXRETRIES);

			if (result.next()) {
				this.modus = OverrideModus.getById(result.getString(1));
				return true;
			} else {
				LOGGER.info("Could not fetch override modus");
				return false;
			}
		} catch (SQLException e) {
			LOGGER.info("Error while trying to fetch licence", e);
			return false;
		}
	}

	public OverrideModus getOverrideModus() {	
		return modus;
	}
	
	public boolean mayBuy() {
		if(modus == OverrideModus.NONE || modus == OverrideModus.BUY_MAX || modus == OverrideModus.DONT_SELL) {
			return true;
		} else {
			LOGGER.info("May not buy because override modus " + modus + " is active");
			return false;
		}
	}
	
	public boolean mayTrade() {
		if(modus != OverrideModus.DO_NOTHING) {
			return true;
		} else {
			LOGGER.info("May not trade because override modus " + modus + " is active");
			return false;
		}
	}
	
	public boolean maySell() {
		if(modus == OverrideModus.NONE || modus == OverrideModus.SELL_MAX || modus == OverrideModus.DONT_BUY) {
			return true;
		} else {
			LOGGER.info("May not sell because override modus " + modus + " is active");
			return false;
		}
	}
	
	public boolean mustSell() {
		if(modus == OverrideModus.SELL_MAX)  {
			LOGGER.info("Must sell because override modus " + modus + " is active");
			return true;
		} else {
			return false;
		}
	}
	
	public boolean mustBuy() {
		if(modus == OverrideModus.BUY_MAX)  {
			LOGGER.info("Must buy because override modus " + modus + " is active");
			return true;
		} else {
			return false;
		}
	}

	@Override
	public long getTimestep() {
		return RETRY_INTERVAL;
	}

	@Override
	public long getLastTick() {
		return lastTick;
	}

	@Override
	public void tick(long tick) {
		if (this.lastFetchMoment + RETRY_INTERVAL < tick) {
			checkOverrideModus();
			this.lastFetchMoment = new Date().getTime();
		}
		
		this.lastTick = tick;
	}
}
