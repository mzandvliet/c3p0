package c3po.macd;

import c3po.*;

import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.util.Date;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import c3po.bitstamp.BitstampSimulationOrderBookDbSource;
import c3po.bitstamp.BitstampSimulationTradeFloor;
import c3po.bitstamp.BitstampSimulationTickerDbSource;
import c3po.clock.ISimulationClock;
import c3po.clock.SimulationClock;
import c3po.node.AggregateNode;
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
	private final static long simulationEndTime = new Date().getTime();
	private final static long simulationStartTime = simulationEndTime - Time.DAYS * 10;

	
	private final static long interpolationTime = 60 * Time.SECONDS;
	private final static long timestep = 20 * Time.SECONDS;
	
	private final static double walletStartUsd = 100.0d;
	private final static double walletStartBtcInUsd = 0.0d;
	
	private final static long graphInterval = 20 * Time.SECONDS;
	
	private final static String botConfig = "{\"timestep\":60000,\"buyAnalysisConfig\":{\"fastPeriod\":2220000,\"slowPeriod\":27720000,\"signalPeriod\":16680000},\"sellAnalysisConfig\":{\"fastPeriod\":8160000,\"slowPeriod\":8880000,\"signalPeriod\":17400000},\"volumeAnalysisConfig\":{\"fastPeriod\":1800000,\"slowPeriod\":3780000,\"signalPeriod\":22800000},\"traderConfig\":{\"minBuyDiffThreshold\":6.223,\"minSellDiffThreshold\":-17.1638,\"buyVolumeThreshold\":0.3376,\"lossCutThreshold\":0.988042,\"sellThresholdRelaxationFactor\":13.83,\"sellPricePeriod\":5280000}}";
	
	//================================================================================
    // Main
    //================================================================================
	
	public static void main(String[] args) throws ClassNotFoundException, SQLException {
		// Set up global signal tree
		
		DbConnection dbConnection = new DbConnection(new InetSocketAddress("c3po.ramjetanvil.com", 3306), "c3po", "D7xpJwzGJEWf5qWB");
		dbConnection.open();
		
		final BitstampSimulationTickerDbSource tickerNode = new BitstampSimulationTickerDbSource(
				timestep,
				interpolationTime,
				dbConnection,
				simulationStartTime,
				simulationEndTime
				);
		
		final BitstampSimulationOrderBookDbSource orderbookNode = new BitstampSimulationOrderBookDbSource(
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
		
		MacdBotConfig config = MacdBotConfig.fromJSON(botConfig);

		
		// Create bot
		
		int botId = Math.abs(new Random().nextInt());
		MacdBot bot = new MacdBot(botId, config, tickerNode.getOutputLast(), tickerNode.getOutputVolume(), wallet, tradeFloor);
		bot.getTraderNode().setVerbose(true);
		LOGGER.debug(bot.getConfig().toJSON());
		
		final AggregateNode p99Last = new AggregateNode(timestep, orderbookNode.getOutputBidPercentile(0), orderbookNode.getOutputAskPercentile(0)); 
		
		// Create loggers
		
		DebugTradeLogger tradeLogger = new DebugTradeLogger();
		bot.addTradeListener(tradeLogger);
		
//		DbTradeLogger dbLogger = new DbTradeLogger(bot, dbConnection);
//		dbLogger.startSession(simulationStartTime);
		
		// Create the grapher
		
		GraphingNode priceChart = new GraphingNode(graphInterval, "Price",
				tickerNode.getOutputLast(),
				p99Last.getOutput(0),
				orderbookNode.getOutputBidPercentile(0),
				orderbookNode.getOutputAskPercentile(0)
				);
		bot.addTradeListener(priceChart);
		
		GraphingNode volumeChart = new GraphingNode(graphInterval, "Volume",
				tickerNode.getOutputVolume()
				);
		
		GraphingNode analysisGrapher = new GraphingNode(graphInterval, "Analysis",
				bot.getBuyAnalysisNode().getOutputDifference(),
				bot.getSellAnalysisNode().getOutputDifference());
		bot.addTradeListener(analysisGrapher);
		
		// Create a clock
		
		ISimulationClock botClock = new SimulationClock(timestep, interpolationTime);
		botClock.addListener(bot);
		botClock.addListener(priceChart);
		botClock.addListener(volumeChart);
		botClock.addListener(analysisGrapher);
		
		// Run the program

		botClock.run(simulationStartTime, simulationEndTime);
		
		dbConnection.close();
		
		
		// Log results
		
		priceChart.pack();
		priceChart.setVisible(true);
		
		volumeChart.pack();
		volumeChart.setVisible(true);
		
		analysisGrapher.pack();
		analysisGrapher.setVisible(true);
		
		tradeLogger.writeLog();
		LOGGER.debug("Num trades: " + tradeLogger.getActions().size() + ", Wallet: " + tradeFloor.getWalletValueInUsd(wallet));
		LOGGER.debug(bot.getConfig().toString());
		LOGGER.debug(bot.getConfig().toJSON());
	}
}
