package c3po.macd;

import java.net.InetSocketAddress;
import java.util.Date;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import c3po.*;
import c3po.Training.*;
import c3po.bitstamp.*;
import c3po.clock.ISimulationClock;
import c3po.clock.SimulationClock;
import c3po.node.GraphingNode;
import c3po.simulation.*;
import c3po.utils.Time;
import c3po.wallet.IWallet;
import c3po.wallet.Wallet;

public class SimpleMacdTrainer {
	private static final Logger LOGGER = LoggerFactory.getLogger(SimpleMacdTrainer.class);
	
	// First timestamp in database: 1384079023000l
	private final static long simulationEndTime = 1387285485000l;
	private final static long simulationStartTime = simulationEndTime - Time.DAYS * 28;
	private final static long simulationLength = Time.DAYS * 4;
	
	// Timing
	private final static long interpolationTime = 2 * Time.MINUTES;
	private final static long timestep = 1 * Time.MINUTES;

	// Simulation and fitness test
	private final static int numEpochs = 100;
	private final static int numSimulationsPerEpoch = 10;
	private final static int numBots = 500;
	
	// Scoring
	private final static int tradeBias = 6;
	
	// Selection
	private final static int numParents = 250;
	private final static int numElites = 50;
	
	// Config mutation ranges
	private final static double mutationChance = 0.1d;
	private final static long minAnalysisPeriod = 1 * Time.MINUTES;
	private final static long maxAnalysisPeriod = 12 * Time.HOURS;
	private final static double minBuyDiffThreshold = 0.0d;
	private final static double maxBuyDiffThreshold = 30.0d;
	private final static double minSellDiffThreshold = -30.0d;
	private final static double maxSellDiffThreshold = 0.0d;
	private final static long minSellPricePeriod = 1 * Time.MINUTES;
	private final static long maxSellPricePeriod = 6 * Time.HOURS;
	private final static double minLossCuttingPercentage = 0.85d;
	private final static double maxLossCuttingPercentage = 1d;
	private final static double minSellThresholdRelaxationFactor = 0d;
	private final static double maxSellThresholdRelaxationFactor = 100d;
	
	// Market context
	private final static double walletStartUsd = 100.0d;
	
	private final static long graphInterval = 5 * Time.MINUTES;

	public static void main(String[] args) {
		
		// Create the simulation context
		
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
		
		ISimulationClock botClock = new SimulationClock(timestep, interpolationTime);
		
		final IWallet wallet = new Wallet(walletStartUsd, 0d);
		
		SimulationContext simContext = new SimulationContext(tickerNode, botClock, tickerNode.getOutputLast(), tickerNode.getOutputVolume(), tradeFloor, wallet);
		
		
		// Create and run the trainer on the context
		
		MacdBotConfig winningConfig = runTrainer(simContext);
		
		// Run the winning bot on the context again
		
		runWinner(winningConfig, simContext);
		
		dbConnection.close();
	}

	private static MacdBotConfig runTrainer(SimulationContext simContext) {
		MacdBotMutatorConfig mutatorConfig = new MacdBotMutatorConfig(
				mutationChance, minAnalysisPeriod,
				maxAnalysisPeriod, minBuyDiffThreshold,
				maxBuyDiffThreshold, minSellDiffThreshold,
				maxSellDiffThreshold, minLossCuttingPercentage,
				minSellPricePeriod, maxSellPricePeriod,
				maxLossCuttingPercentage,
				minSellThresholdRelaxationFactor,
				maxSellThresholdRelaxationFactor);
		
		MacdBotMutator mutator = new MacdBotMutator(mutatorConfig);
		
		GenAlgBotTrainerConfig genAlgConfig = new GenAlgBotTrainerConfig(
				simulationStartTime,
				simulationEndTime,
				simulationLength,
				numEpochs,
				numSimulationsPerEpoch,
				tradeBias,
				numBots,
				numParents,
				numElites);
		
		GenAlgBotTrainer<MacdBotConfig> trainer = new GenAlgBotTrainer<MacdBotConfig>(genAlgConfig, mutator);
		
		IBotFactory<MacdBotConfig> botFactory = new MacdBotFactory(simContext);
		
		MacdBotConfig winningConfig = trainer.train(botFactory, simContext);
		
		return winningConfig;
	}
	
	private static void runWinner(final MacdBotConfig winningConfig, SimulationContext simContext) {		
		LOGGER.debug("Running winner: " + winningConfig.toString());
		
		MacdBot bot = new MacdBot(
				Math.abs(new Random().nextInt()),
				winningConfig,
				simContext.getPriceSignal(),
				simContext.getVolumeSignal(),
				simContext.getWalletInstance(),
				simContext.getTradeFloor());
		bot.getTraderNode().setVerbose(true);
		
		DebugTradeLogger tradeLogger = new DebugTradeLogger();
		bot.addTradeListener(tradeLogger);
		
		// Create the grapher
		
		GraphingNode grapher = new GraphingNode(graphInterval, "MacdBot", 
				simContext.getPriceSignal(),
				bot.getBuyAnalysisNode().getOutputFast(),
				bot.getBuyAnalysisNode().getOutputSlow()
		);
		bot.addTradeListener(grapher);
		
		GraphingNode diffGrapher = new GraphingNode(graphInterval, "Macd", 
				bot.getBuyAnalysisNode().getOutputDifference(),
				bot.getSellAnalysisNode().getOutputDifference()
		);
		bot.addTradeListener(diffGrapher);
		
		// Run
		
		simContext.setSimulationRange(simulationStartTime, simulationEndTime);
		
		ISimulationClock clock = simContext.getClock();
		
		clock.addListener(bot);
		clock.addListener(grapher);
		clock.addListener(diffGrapher);
		
		clock.run(simulationStartTime, simulationEndTime);
		
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
		LOGGER.debug("Ran winner: " + winningConfig.toString());
	}
}
