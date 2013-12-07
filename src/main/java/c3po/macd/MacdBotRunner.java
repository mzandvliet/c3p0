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
import c3po.ISignal;
import c3po.ITradeFloor;
import c3po.ITradeListener;
import c3po.IWallet;
import c3po.Time;

public class MacdBotRunner {
	//================================================================================
    // Static Properties
    //================================================================================
	
	private static final Logger LOGGER = LoggerFactory.getLogger(MacdBotRunner.class);
	
	// Earliest time 1384079023000l
	private final static long simulationStartTime = new Date().getTime() - Time.DAYS * 7;
	private final static long simulationEndTime = new Date().getTime();
	
	private final static long interpolationTime = 2 * Time.MINUTES;
	private final static long timestep = 1 * Time.MINUTES;
	
	private final static double walletStartUsd = 1000.0d;
	private final static double walletStartBtcInUsd = 0.0d;
	
	private final static long graphInterval = 1 * Time.MINUTES;
	
	//================================================================================
    // Main
    //================================================================================
	
	public static void main(String[] args) throws ClassNotFoundException, SQLException {
		// Set up global signal tree
		
		final BitstampSimulationTickerDbSource tickerNode = new BitstampSimulationTickerDbSource(
				timestep,
				interpolationTime,
				new InetSocketAddress("94.208.87.249", 3309),
				"c3po",
				"D7xpJwzGJEWf5qWB",
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
		MacdAnalysisConfig analysisConfig = new MacdAnalysisConfig(
				56 * Time.MINUTES,
				241 * Time.MINUTES,
				262 * Time.MINUTES);
		
		MacdTraderConfig traderConfig = new MacdTraderConfig(
				1.5855,
				-3.8407);
		MacdBotConfig config = new MacdBotConfig(timestep, analysisConfig, traderConfig);
		
		DbConnection dbConnection = new DbConnection(new InetSocketAddress("94.208.87.249", 3309), "c3po", "D7xpJwzGJEWf5qWB");
		dbConnection.open();
		
		// Create the aggregate ticker signal
		AggregateNode bidAskMedianNode = new AggregateNode(timestep, tickerNode.getOutputBid(), tickerNode.getOutputAsk());
		
		// Create bot
		
		int botId = Math.abs(new Random().nextInt());
		MacdBot bot = new MacdBot(botId, config, bidAskMedianNode.getOutput(0), wallet, tradeFloor);
		
		// Create loggers
		
		DebugTradeLogger tradeLogger = new DebugTradeLogger();
		bot.addTradeListener(tradeLogger);
		
		DbTradeLogger dbLogger = new DbTradeLogger(bot, dbConnection);
		dbLogger.startSession(simulationStartTime);
		
//		EmailTradeLogger mailLogger = new EmailTradeLogger("martijn@ramjetanvil.com", "jopast@gmail.com");
//		bot.addTradeListener(mailLogger);
		
		// Create the grapher
		
		GraphingNode grapher = new GraphingNode(graphInterval, "Ticker", 
				tickerNode.getOutputLast(),
				bidAskMedianNode.getOutput(0)
				);
		bot.addTradeListener(grapher);
		
		GraphingNode diffGrapher = new GraphingNode(graphInterval, "Macd", 
				bot.getAnalysisNode().getOutputDifference()
				);
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

		dbConnection.close();
	}
}
