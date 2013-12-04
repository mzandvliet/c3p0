package c3po.production;

import c3po.*;

import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import c3po.bitstamp.BitstampTickerSource;
import c3po.bitstamp.BitstampTickerDbSource;
import c3po.bitstamp.BitstampTradeFloor;
import c3po.EmailTradeLogger;
import c3po.GraphingNode;
import c3po.IClock;
import c3po.ITradeFloor;
import c3po.Time;
import c3po.macd.*;

public class RealtimeBotRunner {

	private static final Logger LOGGER = LoggerFactory.getLogger(RealtimeBotRunner.class);
	private final static String jsonUrl = "http://www.bitstamp.net/api/ticker/";

	private final static long interpolationTime = 2 * Time.MINUTES;
	private final static long timestep = 1 * Time.MINUTES;
	
	private static final double walletDollarStart = 0.0;
	private static final double walletBtcStart = 0.0;
	
	private final static long graphInterval = 1 * Time.MINUTES;

	public static void main(String[] args) throws ClassNotFoundException, SQLException {
			
		/**
		 * Bot Config
		 */
		MacdAnalysisConfig analysisConfig = new MacdAnalysisConfig(
				39 * Time.MINUTES,
				218 * Time.MINUTES,
				273 * Time.MINUTES);
		
		MacdTraderConfig traderConfig = new MacdTraderConfig(
				0.6609,
				-9.6978);
		
		MacdBotConfig config = new MacdBotConfig(timestep, analysisConfig, traderConfig);
		
		// Set up global signal tree
		final BitstampTickerSource tickerNode = new BitstampTickerDbSource(timestep, interpolationTime, new InetSocketAddress("94.208.87.249", 3309), "c3po", "D7xpJwzGJEWf5qWB");

		/**
		 * Tradefloor + Wallet Initialization
		 */
		final IWallet wallet = new Wallet(walletDollarStart, walletBtcStart);
		
		final ITradeFloor tradeFloor =  new BitstampTradeFloor(
				tickerNode.getOutputLast(),
				tickerNode.getOutputBid(),
				tickerNode.getOutputAsk()
		);
		
		// Update the wallet with the real values
		tradeFloor.updateWallet(wallet);
		
		final DebugTradeLogger tradeLogger = new DebugTradeLogger();
		tradeFloor.addTradeListener(tradeLogger);
		
		// Create bot
		MacdBot bot = new MacdBot(config, tickerNode.getOutputLast(), wallet, tradeFloor);
		
		// Log the trades by DB and email
		DbTradeLogger dbTradeLogger = new DbTradeLogger(bot, 0, new InetSocketAddress("94.208.87.249", 3309), "c3po", "D7xpJwzGJEWf5qWB");
		dbTradeLogger.open();
		EmailTradeLogger mailLogger = new EmailTradeLogger("martijn@ramjetanvil.com", "jopast@gmail.com");
		bot.addTradeListener(mailLogger);
		
		// Graph performance in realtime
		
		GraphingNode grapher = new GraphingNode(graphInterval, "Ticker", 
				tickerNode.getOutputLast()
				);
		bot.addTradeListener(grapher);
		
		GraphingNode diffGrapher = new GraphingNode(graphInterval, "Macd", 
				bot.getAnalysisNode().getOutputDifference()
				);
		bot.addTradeListener(diffGrapher);
		
		grapher.pack();
		grapher.setVisible(true); // Show graph *after* simulation because otherwise annotation adding causes exceptions
		
		diffGrapher.pack();
		diffGrapher.setVisible(true); // Show graph *after* simulation because otherwise annotation adding causes exceptions
		
		// Create a clock
		IClock botClock = new RealtimeClock(timestep, Math.max(analysisConfig.slowPeriod, analysisConfig.signalPeriod), interpolationTime);
		botClock.addListener(bot);
		botClock.addListener(grapher);
		botClock.addListener(diffGrapher);
		
		// Run the program
		tickerNode.open();
		botClock.run();
		tickerNode.close();
		
		// Log results
		LOGGER.debug("Num trades: " + tradeLogger.getActions().size() + ", Profit: " + (tradeFloor.getWalletValueInUsd(wallet) - walletDollarStart));
	}
}
