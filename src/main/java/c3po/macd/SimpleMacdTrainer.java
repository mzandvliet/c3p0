package c3po.macd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import c3po.IClock;
import c3po.ITradeFloor;
import c3po.IWallet;
import c3po.SimulationClock;
import c3po.Time;
import c3po.Wallet;
import c3po.Training.GenAlgBotTrainer;
import c3po.Training.GenAlgBotTrainerConfig;
import c3po.bitstamp.BitstampSimulationTradeFloor;
import c3po.bitstamp.BitstampSimulationTickerCsvSource;
import c3po.simulation.SimulationBotRunner;

/*
 * TODO: Finish implementing this, needs fleshed out GenAlgBotTrainer
 * 
 * Trainers need to accept a simulation context, which specifies:
 * 		- Training set
 * 		- Wallet
 * 
 */

public class SimpleMacdTrainer {
private static final Logger LOGGER = LoggerFactory.getLogger(SimulationBotRunner.class);
	
	private final static String csvPath = "resources/bitstamp_ticker_till_20131126.csv";
	private final static long simulationStartTime = 1384079023000l;
	private final static long simulationEndTime = 1385501193000l;
	
	// Timing
	private final static long interpolationTime = 2 * Time.MINUTES;
	private final static long timestep = 1 * Time.MINUTES;

	// Simulation and fitness test
	private final static int numEpochs = 100;
	private final static int numBots = 250;
	
	// Selection
	private final static int numParents = 125;
	private final static int numElites = 10;
	
	// Config mutation ranges
	private final static double mutationChance = 0.5d;
	private final static long minAnalysisPeriod = 1 * Time.MINUTES;
	private final static long maxAnalysisPeriod = 12 * Time.HOURS;
	private final static double minBuyDiffThreshold = -10.0d;
	private final static double maxBuyDiffThreshold = 10.0d;
	private final static double minSellDiffThreshold = -10.0d;
	private final static double maxSellDiffThreshold = 10.0d;
	
	// Market context
	private final static double walletStartUsd = 1000.0d;
	private final static double walletStartBtcInUsd = 0.0d;

	public static void main(String[] args) {
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
		
		// TODO: Needs a simulation context to optimize against
		GenAlgBotTrainer<MacdBotConfig> trainer = new GenAlgBotTrainer<MacdBotConfig>(genAlgConfig, mutator, null, null);
		
		final BitstampSimulationTickerCsvSource tickerNode = new BitstampSimulationTickerCsvSource(timestep, interpolationTime, csvPath);
		
		final ITradeFloor tradeFloor =  new BitstampSimulationTradeFloor(
				tickerNode.getOutputLast(),
				tickerNode.getOutputBid(),
				tickerNode.getOutputAsk()
		);
		
		double walletStartBtc = walletStartBtcInUsd / tickerNode.getOutputLast().getSample(simulationStartTime).value;
		final IWallet wallet = new Wallet(walletStartUsd, walletStartBtc);
		
		// TODO: Run the WINNING BOT again, Graph the results for manual evaluation

		tickerNode.open();
		IClock botClock = new SimulationClock(timestep, simulationStartTime, simulationEndTime, interpolationTime);
		botClock.run();
		tickerNode.close();
	}
}
