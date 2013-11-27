package c3po.simulation;

import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import c3po.*;
import c3po.macd.*;
import c3po.Training.*;
import c3po.bitstamp.*;

public class SimulationBotRunner {
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
	
	public static void main(String[] args) throws ClassNotFoundException, SQLException {
		final BitstampTickerCsvSource tickerNode = new BitstampTickerCsvSource(timestep, interpolationTime, csvPath);
		tickerNode.open();
		
		final ITradeFloor tradeFloor =  new BitstampSimulationTradeFloor(
				tickerNode.getOutputLast(),
				tickerNode.getOutputBid(),
				tickerNode.getOutputAsk()
		);
		
		double walletStartBtc = walletStartBtcInUsd / tickerNode.getOutputLast().getSample(simulationStartTime).value;
		final IWallet wallet = new Wallet(walletStartUsd, walletStartBtc);
		
		MacdBotFactory botFactory = new MacdBotFactory(wallet, tickerNode.getOutputLast(), tradeFloor);
		
		GenAlgBotTrainerConfig genAlgConfig = new GenAlgBotTrainerConfig(
				numEpochs,
				numBots,
				numParents,
				numElites,
				mutationChance);
		MacdBotMutatorConfig mutatorConfig = new MacdBotMutatorConfig(
				mutationChance,
				minAnalysisPeriod,
				maxAnalysisPeriod,
				minBuyDiffThreshold,
				maxBuyDiffThreshold,
				minSellDiffThreshold,
				maxSellDiffThreshold);
		
		MacdBotMutator mutator = new MacdBotMutator(mutatorConfig);
		
		IBotTrainer<MacdBotConfig> trainer = new GenAlgBotTrainer<MacdBotConfig>(genAlgConfig, botFactory, mutator, null);
		
		SelfOptimizingMacdBot bot = new SelfOptimizingMacdBot(timestep, trainer, null);
		
		IClock botClock = new SimulationClock(timestep, simulationStartTime, simulationEndTime, interpolationTime);
		botClock.addListener(bot);
		
		botClock.run();
		
		tickerNode.close();
	}
}
