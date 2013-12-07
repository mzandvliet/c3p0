package c3po.macd;

import java.net.InetSocketAddress;
import java.util.Date;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import c3po.*;
import c3po.Training.*;
import c3po.bitstamp.*;
import c3po.simulation.*;

/* TODO: SimContext.reset() is STILL BROKEN
 *
 * resultSet.first() is likely not doing what I think it is.
 * 
 */

public class SimpleMacdTrainer {
private static final Logger LOGGER = LoggerFactory.getLogger(SimpleMacdTrainer.class);
	
	// First timestamp in database: 1384079023000l
    private final static long simulationStartTime =  new Date().getTime() - Time.DAYS * 7;
	private final static long simulationEndTime = new Date().getTime();
	
	// Timing
	private final static long interpolationTime = 2 * Time.MINUTES;
	private final static long timestep = 1 * Time.MINUTES;

	// Simulation and fitness test
	private final static int numEpochs = 50;
	private final static int numBots = 250;
	
	// Selection
	private final static int numParents = 125;
	private final static int numElites = 10;
	
	// Config mutation ranges
	private final static double mutationChance = 0.25d;
	private final static long minAnalysisPeriod = 1 * Time.MINUTES;
	private final static long maxAnalysisPeriod = 12 * Time.HOURS;
	private final static double minBuyDiffThreshold = -20.0d;
	private final static double maxBuyDiffThreshold = 20.0d;
	private final static double minSellDiffThreshold = -20.0d;
	private final static double maxSellDiffThreshold = 20.0d;
	private final static double minBuyPercentage = 0.3d;
	private final static double maxBuyPercentage = 1d;
	private final static double minSellPercentage = 1d;
	private final static double maxSellPercentage = 1d;
	
	private final static long minBuyBackoffTimer = 1 * Time.MINUTES;
	private final static long maxBuyBackoffTimer = 12 * Time.HOURS;
	private final static long minSellBackoffTimer = 1 * Time.MINUTES;
	private final static long maxSellBackoffTimer = 12 * Time.HOURS;
	
	private final static double minLossCuttingPercentage = 0.0d;
	private final static double maxLossCuttingPercentage = 0.92d;
	
	// Market context
	private final static double walletStartUsd = 500.0d;
	private final static double walletStartBtcInUsd = 500.0d;
	
	private final static long graphInterval = 10 * Time.MINUTES;

	public static void main(String[] args) {
		
		// Create the simulation context
		
		IClock botClock = new SimulationClock(timestep, simulationStartTime, simulationEndTime, interpolationTime);
		
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
		
		SimulationContext simContext = new SimulationContext(tickerNode, tickerNode.getOutputLast(), tradeFloor, wallet, botClock);
		
		
		// Create and run the trainer on the context
		
		MacdBotConfig winningConfig = runTrainer(simContext);
		
		// Run the winning bot on the context again
		
		runWinner(winningConfig, simContext);
		
		tickerNode.close();
	}

	private static MacdBotConfig runTrainer(SimulationContext simContext) {
		MacdBotMutatorConfig mutatorConfig = new MacdBotMutatorConfig(
				mutationChance,
				minAnalysisPeriod, maxAnalysisPeriod,
				minBuyDiffThreshold,  maxBuyDiffThreshold,
				minSellDiffThreshold, maxSellDiffThreshold,
				minBuyPercentage, maxBuyPercentage,
				minSellPercentage, maxSellPercentage,
				minBuyBackoffTimer, maxBuyBackoffTimer,
				minSellBackoffTimer, maxSellBackoffTimer,
				minLossCuttingPercentage, maxLossCuttingPercentage);
		
		MacdBotMutator mutator = new MacdBotMutator(mutatorConfig);
		
		GenAlgBotTrainerConfig genAlgConfig = new GenAlgBotTrainerConfig(
				numEpochs,
				numBots,
				numParents,
				numElites,
				mutationChance);
		
		GenAlgBotTrainer<MacdBotConfig> trainer = new GenAlgBotTrainer<MacdBotConfig>(genAlgConfig, mutator);
		
		
		IBotFactory<MacdBotConfig> botFactory = new MacdBotFactory(simContext);
		
		MacdBotConfig winningConfig = trainer.train(botFactory, simContext);
		
		return winningConfig;
	}
	
	private static void runWinner(final MacdBotConfig winningConfig, SimulationContext simContext) {
		simContext.reset();
		
		LOGGER.debug("Running winner: " + winningConfig.toString());
		
		MacdBot bot = new MacdBot(new Random().nextInt(), winningConfig, simContext.getSignal(), simContext.getWalletInstance(), simContext.getTradeFloor());
		
		DebugTradeLogger tradeLogger = new DebugTradeLogger();
		bot.addTradeListener(tradeLogger);
		
		// Create the grapher
		
		GraphingNode grapher = new GraphingNode(graphInterval, "MacdBot", 
				simContext.getSignal(),
				bot.getAnalysisNode().getOutput(0),
				bot.getAnalysisNode().getOutput(1)
				);
		bot.addTradeListener(grapher);
		
		GraphingNode diffGrapher = new GraphingNode(graphInterval, "Macd", 
				bot.getAnalysisNode().getOutputDifference()
				);
		bot.addTradeListener(diffGrapher);
		
		// Run
		
		IClock clock = simContext.getClock();
		
		clock.addListener(bot);
		clock.addListener(grapher);
		clock.addListener(diffGrapher);
		
		clock.run();
		
		clock.removeListener(bot);
		clock.removeListener(grapher);
		clock.removeListener(diffGrapher);
		
		// Log results
		
		grapher.pack();
		grapher.setVisible(true);
		
		diffGrapher.pack();
		diffGrapher.setVisible(true); 
		
		tradeLogger.writeLog();
		LOGGER.debug("Num trades: " + tradeLogger.getActions().size() + ", Wallet: " + simContext.getTradeFloor().getWalletValueInUsd(bot.getWallet()));
	}
}
