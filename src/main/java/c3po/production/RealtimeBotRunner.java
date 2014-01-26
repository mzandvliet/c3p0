package c3po.production;

import c3po.*;

import java.io.FileInputStream;
import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.util.Date;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import c3po.bitstamp.BitstampSimulationTickerDbSource;
import c3po.bitstamp.BitstampTickerJsonSource;
import c3po.bitstamp.BitstampTickerSourceWrapper;
import c3po.bitstamp.BitstampTradeFloor;
import c3po.clock.IRealtimeClock;
import c3po.clock.ISimulationClock;
import c3po.clock.SimulationClock;
import c3po.db.DbBotSession;
import c3po.db.DbConnection;
import c3po.db.DbLicenceChecker;
import c3po.db.DbOverrideModusChecker;
import c3po.db.DbTradeLogger;
import c3po.EmailTradeLogger;
import c3po.ITradeFloor;
import c3po.macd.*;
import c3po.utils.Time;
import c3po.wallet.IWallet;
import c3po.wallet.Wallet;

public class RealtimeBotRunner {

	private static final String VERSION_IDENTIFIER = "6.1.0";

	private static final Logger LOGGER = LoggerFactory.getLogger(RealtimeBotRunner.class);
	
	private final static long interpolationTime = 2 * Time.MINUTES;
	private final static long timestep = 1 * Time.MINUTES;

	private static DbLicenceChecker dbLicenceChecker;
	private static DbBotSession dbBotSession;
	private static DbOverrideModusChecker dbOverrideModusChecker;
	

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
	    	String dbUser = prop.getProperty("dbUser");
	    	String dbPwd = prop.getProperty("dbPwd");
	    	
	    	DbConnection dbConnection = new DbConnection(new InetSocketAddress("c3po.ramjetanvil.com", 3306), dbUser, dbPwd);
	    	dbConnection.open();	
	    	
			/**
			 * Bot Config
			 */			
			MacdBotConfig config = MacdBotConfig.fromJSON(prop.getProperty("config"));
			
			// Set up a wrapped ticker that is in turn supplied by a SimulationSource and then a RealtimeSource
			BitstampTickerSourceWrapper wrappedSource = new BitstampTickerSourceWrapper();
			
			/**
			 * Tradefloor + Wallet Initialization
			 */
			final ITradeFloor tradeFloor =  new BitstampTradeFloor(
					wrappedSource.getOutputLast(),
					wrappedSource.getOutputBid(),
					wrappedSource.getOutputAsk(),
					(Integer.valueOf(prop.getProperty("tradefloorLimitOrder")).intValue() == 1),
					clientId, apiKey, apiSecret
			);
			
			final IWallet wallet = new Wallet(0d, 0d, 0d, 0d);
			
			// Property "walletReserveUsd" allows you to reserve a portion of your wallet to not be used by the bot
			if(prop.containsKey("walletReserveUsd"))
			  wallet.setConfigUsdReserved(Double.valueOf(prop.getProperty("walletReserveUsd")));
			
			final DebugTradeLogger tradeLogger = new DebugTradeLogger();
			tradeFloor.addTradeListener(tradeLogger);
			
			// Create bot
			MacdBot bot = new MacdBot(botId, config, wrappedSource.getOutputLast(), wrappedSource.getOutputVolume(), wallet, tradeFloor);
			
			// Should not get any trades, but here for the wallet update
			new DbTradeLogger(bot, dbConnection);
	    	dbLicenceChecker = new DbLicenceChecker(bot, apiKey, dbConnection);
	    	dbBotSession = new DbBotSession(bot, dbConnection);
	    	dbOverrideModusChecker = new DbOverrideModusChecker(bot, apiKey, dbConnection);
	    	bot.getTraderNode().setOverrideModusChecker(dbOverrideModusChecker);
			
			// Email Trade Logger
			if(prop.containsKey("emailNotify")) {
				String[] emails = prop.getProperty("emailNotify").split(",");
				EmailTradeLogger mailLogger = new EmailTradeLogger(bot.getId(), emails);
				bot.addTradeListener(mailLogger);
			}
			
			LOGGER.info("Starting bot: " + bot + " V"+VERSION_IDENTIFIER);
			dbBotSession.startSession(VERSION_IDENTIFIER);
			
			preload(wrappedSource, dbConnection, bot);
			run(wrappedSource, dbConnection, bot, tradeFloor, wallet);
			
			dbConnection.close();
			
		} catch (Exception e) {
			LOGGER.error("Critical error in main", e);
		}
	}
	
	private static void preload(BitstampTickerSourceWrapper wrapper, DbConnection dbConnection, MacdBot bot) throws ClassNotFoundException, SQLException {
		LOGGER.debug("Starting data preload...");
		
		MacdBotConfig botConfig = bot.getConfig();
		
		long preloadEndTime = new Date().getTime();
		long preloadDuration = Math.max(botConfig.buyAnalysisConfig.slowPeriod, botConfig.sellAnalysisConfig.signalPeriod);
		long preloadStartTime = preloadEndTime - preloadDuration;
		
		final BitstampSimulationTickerDbSource simulatedTicker = new BitstampSimulationTickerDbSource(timestep, interpolationTime, dbConnection, preloadStartTime, preloadEndTime);
		
		wrapper.setActualSource(simulatedTicker); // Switch the data source of the signal tree to a simulation source
		
		ISimulationClock clock = new SimulationClock(timestep, interpolationTime);
		
		clock.addListener(dbLicenceChecker);
		clock.addListener(bot);
		clock.run(preloadStartTime, preloadEndTime);
		clock.removeListener(bot);
		
		LOGGER.debug("Finished preloading data!");
	}
	
	private static void run(BitstampTickerSourceWrapper wrapper, DbConnection dbConnection, MacdBot bot, ITradeFloor tradeFloor, IWallet wallet) {
		LOGGER.debug("Starting realtime execution...");
		
		BitstampTickerJsonSource realtimeTicker = new BitstampTickerJsonSource(timestep, interpolationTime, "https://www.bitstamp.net:443/api/ticker/");
		
		wrapper.setActualSource(realtimeTicker); // Switch the data source of the signal tree to a real-time source
		
		// Update the wallet with the real values
		tradeFloor.updateWallet(wallet);

		// Create a clock
		IRealtimeClock clock = new RealtimeClock(timestep, interpolationTime);
				
		// Run the program
		clock.addListener(dbLicenceChecker);
		clock.addListener(dbOverrideModusChecker);
		clock.addListener(dbBotSession);
		clock.addListener(bot);
		clock.run();
		clock.removeListener(bot);
		
		LOGGER.debug("Finished realtime execution!");
	}
}
