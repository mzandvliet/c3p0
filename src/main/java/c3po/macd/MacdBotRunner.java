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
import c3po.DbConnection;
import c3po.IClock;
import c3po.ITradeFloor;
import c3po.IWallet;
import c3po.Time;

public class MacdBotRunner {
	//================================================================================
    // Static Properties
    //================================================================================
	
	private static final Logger LOGGER = LoggerFactory.getLogger(MacdBotRunner.class);
	
	// Earliest time 1384079023000l
	private final static long simulationStartTime = 1386676800000l - (Time.DAYS * 8);
	private final static long simulationEndTime = 1386676800000l - (Time.DAYS * 2);
	
	private final static long interpolationTime = 2 * Time.MINUTES;
	private final static long timestep = 1 * Time.MINUTES;
	
	private final static double walletStartUsd = 100.0d;
	private final static double walletStartBtcInUsd = 0.0d;
	
	private final static long graphInterval = 10 * Time.MINUTES;
	
	//================================================================================
    // Main
    //================================================================================
	
	public static void main(String[] args) throws ClassNotFoundException, SQLException {
		// Set up global signal tree
		
		DbConnection dbConnection = new DbConnection(new InetSocketAddress("94.208.87.249", 3309), "c3po", "D7xpJwzGJEWf5qWB");
		
		final BitstampSimulationTickerDbSource tickerNode = new BitstampSimulationTickerDbSource(
				timestep,
				interpolationTime,
				dbConnection,
				simulationStartTime,
				simulationEndTime
				);
		tickerNode.open();
		
		final ITradeFloor tradeFloor =  new BitstampSimulationTradeFloor(
				tickerNode.getOutputLast(),
				tickerNode.getOutputBid(),
				tickerNode.getOutputAsk()
		);
		
		double walletStartBtc = walletStartBtcInUsd / tickerNode.getOutputLast().getSample(simulationStartTime).value;
		final IWallet wallet = new Wallet(walletStartUsd, walletStartBtc);
		
		// Create bot config
		MacdAnalysisConfig buyAnalysisConfig = new MacdAnalysisConfig(
				47 * Time.MINUTES,
				410 * Time.MINUTES,
				137 * Time.MINUTES);
		
		MacdAnalysisConfig sellAnalysisConfig = new MacdAnalysisConfig(
				21 * Time.MINUTES,
				219 * Time.MINUTES,
				356 * Time.MINUTES);
		
		MacdTraderConfig traderConfig = new MacdTraderConfig(
				5.3,
				-12.6,
				0.85);
		
		MacdBotConfig config = new MacdBotConfig(timestep, buyAnalysisConfig, sellAnalysisConfig, traderConfig);
		
//		DbConnection dbConnection = new DbConnection(new InetSocketAddress("94.208.87.249", 3309), "c3po", "D7xpJwzGJEWf5qWB");
//		dbConnection.open();
		
		// Create the aggregate ticker signal
		AggregateNode bidAskMedianNode = new AggregateNode(timestep, tickerNode.getOutputBid(), tickerNode.getOutputAsk());
		
		// Create bot
		
		int botId = Math.abs(new Random().nextInt());
		MacdBot bot = new MacdBot(botId, config, bidAskMedianNode.getOutput(0), wallet, tradeFloor);
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
				tickerNode.getOutputLast(),
				bidAskMedianNode.getOutput(0)
				);
		bot.addTradeListener(grapher);
		
		GraphingNode diffGrapher = new GraphingNode(graphInterval, "Macd", 
				bot.getBuyAnalysisNode().getOutputDifference(),
				bot.getSellAnalysisNode().getOutputDifference());
		
		bot.addTradeListener(diffGrapher);
		
		// Create a clock
		
		IClock botClock = new SimulationClock(timestep, simulationStartTime, simulationEndTime, interpolationTime);
		botClock.addListener(bot);
		botClock.addListener(grapher);
		botClock.addListener(diffGrapher);
		
		// Run the program

		botClock.run();
		
		tickerNode.close();
		
		
		// Log results
		
		grapher.pack();
		grapher.setVisible(true); // Show graph *after* simulation because otherwise annotation adding causes exceptions
		
		diffGrapher.pack();
		diffGrapher.setVisible(true); // Show graph *after* simulation because otherwise annotation adding causes exceptions
		
		tradeLogger.writeLog();
		LOGGER.debug("Num trades: " + tradeLogger.getActions().size() + ", Wallet: " + tradeFloor.getWalletValueInUsd(wallet));

//		dbConnection.close();
	}
}
