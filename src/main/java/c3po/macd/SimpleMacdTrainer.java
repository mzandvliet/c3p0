package c3po.macd;

import java.net.InetSocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import c3po.DebugTradeLogger;
import c3po.GraphingNode;
import c3po.IClock;
import c3po.ITradeFloor;
import c3po.IWallet;
import c3po.SimulationClock;
import c3po.Time;
import c3po.Wallet;
import c3po.Training.GenAlgBotTrainer;
import c3po.Training.GenAlgBotTrainerConfig;
import c3po.Training.IBotFactory;
import c3po.bitstamp.BitstampSimulationTickerDbSource;
import c3po.bitstamp.BitstampSimulationTradeFloor;
import c3po.simulation.SimulationBotRunner;
import c3po.simulation.SimulationContext;

/*
 * TODO:
 * 
 * - Refactor simulationContext into a clearer and more reusable concept
 * 
 */

public class SimpleMacdTrainer {
private static final Logger LOGGER = LoggerFactory.getLogger(SimulationBotRunner.class);
	
	private final static long simulationStartTime = 1384079023000l;
	private final static long simulationEndTime = 1384509600000l;
	
	// Timing
	private final static long interpolationTime = 2 * Time.MINUTES;
	private final static long timestep = 1 * Time.MINUTES;

	// Simulation and fitness test
	private final static int numEpochs = 1;
	private final static int numBots = 250;
	
	// Selection
	private final static int numParents = 125;
	private final static int numElites = 10;
	
	// Config mutation ranges
	private final static double mutationChance = 0.5d;
	private final static long minAnalysisPeriod = 1 * Time.MINUTES;
	private final static long maxAnalysisPeriod = 6 * Time.HOURS;
	private final static double minBuyDiffThreshold = -10.0d;
	private final static double maxBuyDiffThreshold = 10.0d;
	private final static double minSellDiffThreshold = -10.0d;
	private final static double maxSellDiffThreshold = 10.0d;
	
	// Market context
	private final static double walletStartUsd = 1000.0d;
	private final static double walletStartBtcInUsd = 0.0d;
	
	private final static long graphInterval = 60 * Time.MINUTES;

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
				minAnalysisPeriod,
				maxAnalysisPeriod,
				minBuyDiffThreshold,
				maxBuyDiffThreshold,
				minSellDiffThreshold,
				maxSellDiffThreshold);
		
		MacdBotMutator mutator = new MacdBotMutator(mutatorConfig);
		
		GenAlgBotTrainerConfig genAlgConfig = new GenAlgBotTrainerConfig(
				numEpochs,
				numBots,
				numParents,
				numElites,
				mutationChance);
		
		
		IBotFactory<MacdBotConfig> botFactory = new MacdBotFactory(simContext);
		
		GenAlgBotTrainer<MacdBotConfig> trainer = new GenAlgBotTrainer<MacdBotConfig>(genAlgConfig, mutator, botFactory, simContext);
		MacdBotConfig winningConfig = trainer.train();
		
		return winningConfig;
	}
	
	private static void runWinner(final MacdBotConfig winningConfig, SimulationContext context) {
		context.reset();
		
		LOGGER.debug("Running bot: " + winningConfig.toString());
		
		MacdBot winningBot = new MacdBot(winningConfig, context.getSignal(), context.getWalletInstance(), context.getTradeFloor());
		
		DebugTradeLogger tradeLogger = new DebugTradeLogger();
		winningBot.addTradeListener(tradeLogger);
		
		// Create the grapher
		
		GraphingNode grapher = new GraphingNode(graphInterval, "MacdBot", 
				context.getSignal(),
				winningBot.getAnalysisNode().getOutput(0),
				winningBot.getAnalysisNode().getOutput(1)
				);
		winningBot.addTradeListener(grapher);
		
		// Run
		
		IClock clock = context.getClock();
		
		clock.addListener(winningBot);
		clock.addListener(grapher);
		
		clock.run();
		
		clock.removeListener(winningBot);
		clock.removeListener(grapher);
		
		// Log results
		
		grapher.pack();
		grapher.setVisible(true);
		
		tradeLogger.writeLog();
		LOGGER.debug("Num trades: " + tradeLogger.getActions().size() + ", Wallet: " + context.getTradeFloor().getWalletValueInUsd(winningBot.getWallet()));
	}
}
