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
import c3po.EmailTradeLogger;
import c3po.ITradeFloor;
import c3po.macd.*;
import c3po.utils.Time;
import c3po.wallet.IWallet;
import c3po.wallet.Wallet;

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
			
			final IWallet wallet = new Wallet(0d, 0d);
			
			final DebugTradeLogger tradeLogger = new DebugTradeLogger();
			tradeFloor.addTradeListener(tradeLogger);
			
			// Create bot
			MacdBot bot = new MacdBot(botId, config, wrappedSource.getOutputLast(), wrappedSource.getOutputVolume(), wallet, tradeFloor);
			LOGGER.info("Starting bot: " + bot);
			
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
		
		clock.addListener(bot);
		clock.run(preloadStartTime, preloadEndTime);
		clock.removeListener(bot);
		
		LOGGER.debug("Finished preloading data!");
	}
	
	private static void run(BitstampTickerSourceWrapper wrapper, DbConnection dbConnection, MacdBot bot, ITradeFloor tradeFloor, IWallet wallet) {
		LOGGER.debug("Starting realtime execution...");
		
		BitstampTickerJsonSource realtimeTicker = new BitstampTickerJsonSource(timestep, interpolationTime, "http://www.bitstamp.net/api/ticker/");
		
		wrapper.setActualSource(realtimeTicker); // Switch the data source of the signal tree to a real-time source
		
		// Update the wallet with the real values
		tradeFloor.updateWallet(wallet);
		
		// Log the trades by DB and email
		new DbTradeLogger(bot, dbConnection);
		
		//dbTradeLogger.startSession(new Date().getTime());
		EmailTradeLogger mailLogger = new EmailTradeLogger(bot.getId(), "martijn@ramjetanvil.com", "jopast@gmail.com");
		bot.addTradeListener(mailLogger);
		
		// Create a clock
		IRealtimeClock clock = new RealtimeClock(timestep, interpolationTime);
				
		// Run the program
		clock.addListener(bot);
		clock.run();
		clock.removeListener(bot);
		
		LOGGER.debug("Finished realtime execution!");
	}
}
