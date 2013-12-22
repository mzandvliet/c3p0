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
	private final static long simulationEndTime = new Date().getTime() - (Time.DAYS * 11);
	private final static long simulationStartTime = simulationEndTime - (Time.DAYS * 14);
	
	private final static long interpolationTime = 60 * Time.SECONDS;
	private final static long timestep = 20 * Time.SECONDS;
	
	private final static double walletStartUsd = 100.0d;
	private final static double walletStartBtcInUsd = 0.0d;
	
	private final static long graphInterval = 20 * Time.SECONDS;
	
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
		final IWallet wallet = new Wallet(walletStartUsd, walletStartBtc, 0d, 0d);
		
		MacdBotConfig config = MacdBotConfig.fromJSON("{\"timestep\":60000,\"buyAnalysisConfig\":{\"fastPeriod\":1817739,\"slowPeriod\":24392209,\"signalPeriod\":15095783},\"sellAnalysisConfig\":{\"fastPeriod\":4155728,\"slowPeriod\":11868447,\"signalPeriod\":23520350},\"volumeAnalysisConfig\":{\"fastPeriod\":328280,\"slowPeriod\":739594,\"signalPeriod\":10506379},\"traderConfig\":{\"minBuyDiffThreshold\":6.563577371206275,\"minSellDiffThreshold\":-28.9950292233447,\"buyVolumeThreshold\":6.186013706384239,\"lossCutThreshold\":0.9917438050616942,\"sellThresholdRelaxationFactor\":27.19466277950374,\"sellPricePeriod\":5599742}}");

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
				tickerNode.getOutputVolume()
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
