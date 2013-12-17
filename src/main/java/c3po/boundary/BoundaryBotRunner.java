package c3po.boundary;

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
import c3po.macd.MacdAnalysisConfig;
import c3po.node.GraphingNode;
import c3po.utils.Time;
import c3po.wallet.IWallet;
import c3po.wallet.Wallet;
import c3po.DbConnection;
import c3po.ITradeFloor;

public class BoundaryBotRunner {
	//================================================================================
    // Static Properties
    //================================================================================
	
	private static final Logger LOGGER = LoggerFactory.getLogger(BoundaryBotRunner.class);
	
	// Earliest time 1384079023000l
	private final static long simulationEndTime = new Date().getTime();
	private final static long simulationStartTime = simulationEndTime - (Time.DAYS * 21);
	
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
				51 * Time.MINUTES,
				222 * Time.MINUTES,
				106 * Time.MINUTES);
		
		BoundaryTraderConfig traderConfig = new BoundaryTraderConfig(
				5,
				0.96);
		
		BoundaryBotConfig config = new BoundaryBotConfig(timestep, buyAnalysisConfig, traderConfig);
		
//		DbConnection dbConnection = new DbConnection(new InetSocketAddress("94.208.87.249", 3309), "c3po", "D7xpJwzGJEWf5qWB");
//		dbConnection.open();
		
		// Create bot
		
		int botId = Math.abs(new Random().nextInt());
		BoundaryBot bot = new BoundaryBot(botId, config, tickerNode.getOutputLast(), tickerNode.getOutputVolume(), wallet, tradeFloor);
		bot.getTraderNode().setVerbose(true);
		
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
				bot.getBuyAnalysisNode().getOutputDifference());
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

//		dbConnection.close();
	}
}
