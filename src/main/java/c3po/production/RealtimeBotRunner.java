package c3po.production;

import c3po.*;

import java.io.FileInputStream;
import java.net.InetSocketAddress;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import c3po.bitstamp.BitstampTickerSource;
import c3po.bitstamp.BitstampTickerDbSource;
import c3po.bitstamp.BitstampTradeFloor;
import c3po.EmailTradeLogger;
import c3po.IClock;
import c3po.ITradeFloor;
import c3po.Time;
import c3po.macd.*;

public class RealtimeBotRunner {

	private static final Logger LOGGER = LoggerFactory.getLogger(RealtimeBotRunner.class);

	private final static long interpolationTime = 2 * Time.MINUTES;
	private final static long timestep = 1 * Time.MINUTES;

	public static void main(String[] args) {
		 
		try {
			if(args.length != 1)
				throw new RuntimeException("Please provide the config file as argument");
			
	        // Load a properties file
			Properties prop = new Properties();
			prop.load(new FileInputStream(args[0]));
	    	int botId = Integer.valueOf(prop.getProperty("botId"));
	    	int clientId = Integer.valueOf(prop.getProperty("clientId"));
	    	String apiKey = prop.getProperty("apiKey");
	    	String apiSecret = prop.getProperty("apiSecret");
	    	
	    	DbConnection dbConnection = new DbConnection(new InetSocketAddress("94.208.87.249", 3309), "c3po", "D7xpJwzGJEWf5qWB");
	    	dbConnection.open();	
	    	
			/**
			 * Bot Config
			 */
			MacdAnalysisConfig buyAnalysisConfig = new MacdAnalysisConfig(
					Integer.valueOf(prop.getProperty("macdBuyFast")) * Time.MINUTES,
					Integer.valueOf(prop.getProperty("macdBuySlow")) * Time.MINUTES,
					Integer.valueOf(prop.getProperty("macdBuySignal")) * Time.MINUTES);

			MacdAnalysisConfig sellAnalysisConfig = new MacdAnalysisConfig(
					Integer.valueOf(prop.getProperty("macdSellFast")) * Time.MINUTES,
					Integer.valueOf(prop.getProperty("macdSellSlow")) * Time.MINUTES,
					Integer.valueOf(prop.getProperty("macdSellSignal")) * Time.MINUTES);
			
			MacdTraderConfig traderConfig = new MacdTraderConfig(
					Double.valueOf(prop.getProperty("macdMinBuyThreshold")),
					Double.valueOf(prop.getProperty("macdMinSellThreshold")),
					0,
					0
			);
			
			MacdBotConfig config = new MacdBotConfig(timestep, buyAnalysisConfig, sellAnalysisConfig, traderConfig);
			
			// Set up global signal tree
			final BitstampTickerSource tickerNode = new BitstampTickerDbSource(timestep, interpolationTime, dbConnection);
	
			/**
			 * Tradefloor + Wallet Initialization
			 */
			final IWallet wallet = new Wallet(0d, 0d);
			
			final ITradeFloor tradeFloor =  new BitstampTradeFloor(
					tickerNode.getOutputLast(),
					tickerNode.getOutputBid(),
					tickerNode.getOutputAsk(),
					(Integer.valueOf(prop.getProperty("tradefloorLimitOrder")).intValue() == 1),
					clientId, apiKey, apiSecret
			);
			
			// Update the wallet with the real values
			tradeFloor.updateWallet(wallet);
			
			final DebugTradeLogger tradeLogger = new DebugTradeLogger();
			tradeFloor.addTradeListener(tradeLogger);
			
			// Create bot
			MacdBot bot = new MacdBot(botId, config, tickerNode.getOutputLast(), tickerNode.getOutputVolume(), wallet, tradeFloor);
			LOGGER.info("Starting bot: " + bot);
			
			// Log the trades by DB and email
			DbTradeLogger dbTradeLogger = new DbTradeLogger(bot, dbConnection);
			
			//dbTradeLogger.startSession(new Date().getTime());
			EmailTradeLogger mailLogger = new EmailTradeLogger(bot.getId(), "martijn@ramjetanvil.com", "jopast@gmail.com");
			bot.addTradeListener(mailLogger);
			
			// Create a clock
			IRealtimeClock botClock = new RealtimeClock(timestep, Math.max(buyAnalysisConfig.slowPeriod, buyAnalysisConfig.signalPeriod), interpolationTime);
			botClock.addListener(bot);
			
			// Run the program
			tickerNode.open();
			botClock.run();
			tickerNode.close();
			
		} catch (Exception e) {
			LOGGER.error("Critical error in main", e);
		}
	}
}
