package c3po.composite;

import c3po.*;

import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import c3po.bitstamp.BitstampSimulationOrderBookDbSource;
import c3po.bitstamp.BitstampSimulationTradeFloor;
import c3po.bitstamp.BitstampSimulationTickerDbSource;
import c3po.clock.ISimulationClock;
import c3po.clock.SimulationClock;
import c3po.macd.MacdAnalysisConfig;
import c3po.macd.MacdAnalysisNode;
import c3po.macd.MacdAnalysisToTradeAdviceNode;
import c3po.macd.MacdBotConfig;
import c3po.node.AggregateNode;
import c3po.node.GraphingNode;
import c3po.utils.Time;
import c3po.wallet.IWallet;
import c3po.wallet.Wallet;
import c3po.DbConnection;
import c3po.ITradeFloor;

public class CompositeBotRunner {
	//================================================================================
    // Static Properties
    //================================================================================
	
	private static final Logger LOGGER = LoggerFactory.getLogger(CompositeBotRunner.class);
	
	// Earliest time 1384079023000l
	private final static long simulationEndTime = new Date().getTime();
	private final static long simulationStartTime = simulationEndTime - Time.DAYS * 10;

	
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
		
		DbConnection dbConnection = new DbConnection(new InetSocketAddress("c3po.ramjetanvil.com", 3306), "c3po", "D7xpJwzGJEWf5qWB");
		dbConnection.open();
		
		// Ticker Source
		final BitstampSimulationTickerDbSource tickerNode = new BitstampSimulationTickerDbSource(
				timestep,
				interpolationTime,
				dbConnection,
				simulationStartTime,
				simulationEndTime
				);
		
		// Orderbook Source
		final BitstampSimulationOrderBookDbSource orderbookNode = new BitstampSimulationOrderBookDbSource(
				timestep,
				interpolationTime,
				dbConnection,
				simulationStartTime,
				simulationEndTime
				);
		
		// Tradefloor
		final ITradeFloor tradeFloor =  new BitstampSimulationTradeFloor(
				tickerNode.getOutputLast(),
				tickerNode.getOutputBid(),
				tickerNode.getOutputAsk()
		);
		
		double walletStartBtc = walletStartBtcInUsd / tickerNode.getOutputLast().getSample(simulationStartTime).value;
		final IWallet wallet = new Wallet(walletStartUsd, walletStartBtc, 0d, 0d);
		
		CompositeBotConfig config = new CompositeBotConfig(timestep, 5, -5);
		
		// Components (components could be individually trained)
		MacdAnalysisNode macdPriceVolume10BTCBid = new MacdAnalysisNode(timestep, orderbookNode.getOutputBidPercentile(99), new MacdAnalysisConfig(10, 20, 30));
		MacdAnalysisNode macdPriceVolume10BTCAsk = new MacdAnalysisNode(timestep, orderbookNode.getOutputBidPercentile(99), new MacdAnalysisConfig(10, 20, 30));
		MacdAnalysisNode macdP99Bid = new MacdAnalysisNode(timestep, orderbookNode.getOutputBidPercentile(99), new MacdAnalysisConfig(10, 20, 30));
		MacdAnalysisNode macdP99Ask = new MacdAnalysisNode(timestep, orderbookNode.getOutputBidPercentile(99), new MacdAnalysisConfig(10, 20, 30));
		
		// Convert Components to TradeAdvice Components
		MacdAnalysisToTradeAdviceNode macdP99BidAdvice = new MacdAnalysisToTradeAdviceNode(timestep, macdPriceVolume10BTCBid.getOutputDifference(), 1d, 1d);
		MacdAnalysisToTradeAdviceNode macdP98BidAdvice = new MacdAnalysisToTradeAdviceNode(timestep, macdPriceVolume10BTCAsk.getOutputDifference(), 1d, 1d);
		MacdAnalysisToTradeAdviceNode macdP97BidAdvice = new MacdAnalysisToTradeAdviceNode(timestep, macdP99Bid.getOutputDifference(), 1d, 1d);
		
		// How much the bot is listening to the seperate advices
		List<WeightedTradeAdviceSignal> weightedTradeAdviceSignals = new LinkedList<WeightedTradeAdviceSignal>();
		weightedTradeAdviceSignals.add(new WeightedTradeAdviceSignal(macdP99BidAdvice, 1, 1));
		weightedTradeAdviceSignals.add(new WeightedTradeAdviceSignal(macdP98BidAdvice, 1, 1));
		weightedTradeAdviceSignals.add(new WeightedTradeAdviceSignal(macdP97BidAdvice, 1, 1));

		// Create bot
		int botId = Math.abs(new Random().nextInt());
		CompositeBot bot = new CompositeBot(botId, config, weightedTradeAdviceSignals, wallet, tradeFloor);
		
		// Create loggers
		
		DebugTradeLogger tradeLogger = new DebugTradeLogger();
		bot.addTradeListener(tradeLogger);
		
//		DbTradeLogger dbLogger = new DbTradeLogger(bot, dbConnection);
//		dbLogger.startSession(simulationStartTime);
		
		// Create the grapher
		
		GraphingNode priceChart = new GraphingNode(graphInterval, "Price",
				tickerNode.getOutputLast(),
				orderbookNode.getOutputBidPercentile(0),
				orderbookNode.getOutputAskPercentile(0)
				);
		bot.addTradeListener(priceChart);
		
		GraphingNode volumeChart = new GraphingNode(graphInterval, "Volume",
				tickerNode.getOutputVolume()
				);
		
		// Create a clock
		
		ISimulationClock botClock = new SimulationClock(timestep, interpolationTime);
		botClock.addListener(bot);
		botClock.addListener(priceChart);
		botClock.addListener(volumeChart);
		
		// Run the program

		botClock.run(simulationStartTime, simulationEndTime);
		
		dbConnection.close();
		
		
		// Log results
		
		priceChart.pack();
		priceChart.setVisible(true);
		
		tradeLogger.writeLog();
		LOGGER.debug("Num trades: " + tradeLogger.getActions().size() + ", Wallet: " + tradeFloor.getWalletValueInUsd(wallet));
		LOGGER.debug(bot.getConfig().toString());
	}
}
