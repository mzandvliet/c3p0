package c3po.macd;

import c3po.*;

import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.util.Date;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import c3po.bitstamp.BitstampSimulationTradeFloor;
import c3po.bitstamp.BitstampSimulationTickerDbSource;
import c3po.clock.ISimulationClock;
import c3po.clock.SimulationClock;
import c3po.node.GraphingNode;
import c3po.utils.Time;
import c3po.wallet.IWallet;
import c3po.wallet.Wallet;
import c3po.DbConnection;
import c3po.ITradeFloor;

public class MacdBotRunner {
	//================================================================================
    // Static Properties
    //================================================================================
	
	private static final Logger LOGGER = LoggerFactory.getLogger(MacdBotRunner.class);
	
	// Earliest time 1384079023000l
	private final static long simulationEndTime = new Date().getTime() - (Time.DAYS * 0);
	private final static long simulationStartTime = simulationEndTime - (Time.DAYS * 14);
	
	private final static long interpolationTime = 2 * Time.MINUTES;
	private final static long timestep = 1 * Time.MINUTES;
	
	private final static double walletStartUsd = 100.0d;
	private final static double walletStartBtcInUsd = 0.0d;
	
	private final static long graphInterval = 1 * Time.MINUTES;
	
	//================================================================================
    // Main
    //================================================================================
	
	public static void main(String[] args) throws ClassNotFoundException, SQLException {
		// Set up global signal tree
		
		DbConnection dbConnection = new DbConnection(new InetSocketAddress("94.208.87.249", 3309), "c3po", "D7xpJwzGJEWf5qWB");
		dbConnection.open();
		
		final BitstampSimulationTickerDbSource tickerNode = new BitstampSimulationTickerDbSource(
				timestep,
				interpolationTime,
				dbConnection,
				simulationStartTime,
				simulationEndTime
				);
		
		final ITradeFloor tradeFloor =  new BitstampSimulationTradeFloor(
				tickerNode.getOutputLast(),
				tickerNode.getOutputBid(),
				tickerNode.getOutputAsk()
		);
		
		double walletStartBtc = walletStartBtcInUsd / tickerNode.getOutputLast().getSample(simulationStartTime).value;
		final IWallet wallet = new Wallet(walletStartUsd, walletStartBtc);
		
		// Create bot config
		MacdAnalysisConfig buyAnalysisConfig = new MacdAnalysisConfig(
				29 * Time.MINUTES,
				381 * Time.MINUTES,
				168 * Time.MINUTES);
		
		MacdAnalysisConfig sellAnalysisConfig = new MacdAnalysisConfig(
				55 * Time.MINUTES,
				199 * Time.MINUTES,
				207 * Time.MINUTES);
		
		MacdTraderConfig traderConfig = new MacdTraderConfig(
				7.1959,
				-25.4469,
				84 * Time.MINUTES,
				0.989084,
				19.35);
		
		MacdBotConfig config = new MacdBotConfig(timestep, buyAnalysisConfig, sellAnalysisConfig, traderConfig);
		
//		MacdBotConfig config = MacdBotConfig.fromJSON("{\"timestep\":60000,\"buyAnalysisConfig\":{\"fastPeriod\":2477601,\"slowPeriod\":23330393,\"signalPeriod\":16324136},\"sellAnalysisConfig\":{\"fastPeriod\":9620180,\"slowPeriod\":11977965,\"signalPeriod\":11944022},\"traderConfig\":{\"minBuyDiffThreshold\":17.210540909606326,\"minSellDiffThreshold\":-7.55726726638008,\"lossCutThreshold\":0.9331220937626041,\"sellThresholdRelaxationFactor\":33.56888487241436,\"sellPricePeriod\":347074}}");

//		DbConnection dbConnection = new DbConnection(new InetSocketAddress("94.208.87.249", 3309), "c3po", "D7xpJwzGJEWf5qWB");
//		dbConnection.open();
		
		// Create bot
		
		int botId = Math.abs(new Random().nextInt());
		MacdBot bot = new MacdBot(botId, config, tickerNode.getOutputLast(), tickerNode.getOutputVolume(), wallet, tradeFloor);
		bot.getTraderNode().setVerbose(true);
		LOGGER.debug(bot.getConfig().toJSON());
		
		// Create loggers
		
		DebugTradeLogger tradeLogger = new DebugTradeLogger();
		bot.addTradeListener(tradeLogger);
		
//		DbTradeLogger dbLogger = new DbTradeLogger(bot, dbConnection);
//		dbLogger.startSession(simulationStartTime);
		
//		EmailTradeLogger mailLogger = new EmailTradeLogger("martijn@ramjetanvil.com", "jopast@gmail.com");
//		bot.addTradeListener(mailLogger);
		
		// Create the grapher
		
		GraphingNode grapher = new GraphingNode(graphInterval, "Ticker",
				tickerNode.getOutputLast()
				);
		bot.addTradeListener(grapher);
		
		GraphingNode volumeGrapher = new GraphingNode(graphInterval, "Volume",
				tickerNode.getOutputVolume(),
				bot.getBuyAnalysisNode().getOutputVolume()
				);
		
		GraphingNode diffGrapher = new GraphingNode(graphInterval, "Macd",
				bot.getBuyAnalysisNode().getOutputDifference(),
				bot.getSellAnalysisNode().getOutputDifference());
		bot.addTradeListener(diffGrapher);
		
		// Create a clock
		
		ISimulationClock botClock = new SimulationClock(timestep, interpolationTime);
		botClock.addListener(bot);
		botClock.addListener(grapher);
		botClock.addListener(volumeGrapher);
		botClock.addListener(diffGrapher);
		
		// Run the program

		botClock.run(simulationStartTime, simulationEndTime);
		
		dbConnection.close();
		
		
		// Log results
		
		grapher.pack();
		grapher.setVisible(true);
		
		volumeGrapher.pack();
		volumeGrapher.setVisible(true);
		
		diffGrapher.pack();
		diffGrapher.setVisible(true);
		
		tradeLogger.writeLog();
		LOGGER.debug("Num trades: " + tradeLogger.getActions().size() + ", Wallet: " + tradeFloor.getWalletValueInUsd(wallet));
		LOGGER.debug(bot.getConfig().toString());
		LOGGER.debug(bot.getConfig().toJSON());

//		dbConnection.close();
	}
}
