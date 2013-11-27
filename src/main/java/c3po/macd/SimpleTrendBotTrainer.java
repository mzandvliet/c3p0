package c3po.macd;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import c3po.bitstamp.BitstampTickerSource;
import c3po.bitstamp.BitstampSimulationTradeFloor;
import c3po.DebugTradeLogger;
import c3po.IBot;
import c3po.IClock;
import c3po.ITradeFloor;
import c3po.IWallet;
import c3po.SimulationClock;
import c3po.Time;
import c3po.Wallet;
import c3po.bitstamp.BitstampTickerCsvSource;

/**
 * This finds optimal MacdBot configurations for a given simulation dataset.
 * 
 * 
 * 
 * Todo:
 * 
 * - Bots luuuuurrve to hold way too long-term positions, make daytraders.
 * 		- Build explicit risk management into macdTrader (with volatility node)
 * 		- Build risk scoring into genetic algorithm
 * 
 * - Early out if a convergence threshold is reached
 * 
 * - Look into Particle Swarm Optimization techniques
 * - Create general optimizers for bots with any kind of configuration space (see below)
 * 
 * - This would be a lot cleaner with the following network architecture changes
 * 		- Binding input signal dynamically (outside of constructor)
 * 		- Bots having a private wallet but sharing a tradefloor
 * 		- Then you could return bots from simulateEpoch and sort performance externally
 * 
 * - Make IBot<TConfig> interface with getConfig(), and IBotTrainer<IBot<TConfig>> interface
 * 		- This makes genetic algorithm optimization trivial since all you have to do is implement the mutate() and related functions for your IConfig
 * 		- Then with simple helper methods for creating random values within ranges, you've got everything you need
 * 		- Then you can also have SelfOptimizingBot<IBot<TConfig>>, which can optimize behaviour of any bot type while running
 * 
 * Notes:
 * 
 * - Bot timestep is not affected by genetic modification. It is a constant.
 */

public class SimpleTrendBotTrainer {
	private static final Logger LOGGER = LoggerFactory.getLogger(SimpleTrendBotTrainer.class);
	
	private final static String csvPath = "resources/bitstamp_ticker_till_20131122_crashed.csv";
	private final static long simulationStartTime = 1384079023000l;
	private final static long simulationEndTime = 1385192429000l; 
	
	// Timing
	private final static long timestep = 60000; // Because right now we're keeping it constant, and data sampling rate is ~1 minute
	private final static long interpolationTime = 120000;

	// Simulation and fitness test
	private final static int numEpochs = 100;
	private final static int numBots = 250;
	
	// Selection
	private final static int numParents = 100;
	private final static int numElites = 5;
	private final static double mutationChance = 0.33d;
	
	// Config mutation ranges
	private final static long minAnalysisPeriod = 1 * Time.MINUTES;
	private final static long maxAnalysisPeriod = 60 * Time.MINUTES;
	private final static long minBackoffPeriod = 1 * Time.MINUTES;
	private final static long maxBackoffPeriod = 60 * Time.MINUTES;
	private final static double minTransactionAmount = 0.01d;
	private final static double maxTransactionAmount = 1d; // NOTE: Never exceed 1.0, lol
	private final static double minDiffThreshold = -2.0d;
	private final static double maxDiffThreshold = 2.0d;
	
	// Market context
	private final static double walletStartDollars = 500.0;
	private final static double walletStartBtcInUsd = 500.0;

	private static final long window = 30 * Time.MINUTES;
	
	public static void main(String[] args) throws ClassNotFoundException, SQLException {
		SimpleTrendBotTrainer trainer = new SimpleTrendBotTrainer();
		trainer.run();		
	}
	
	public void run() {
		List<MacdTraderConfig> configs = createRandomConfigs(numBots);
		
		for (int i = 0; i < numEpochs; i++) {
			List<MacdTraderConfig> sortedConfigs = simulateEpoch(configs, i);
			List<MacdTraderConfig> winners = sortedConfigs.subList(0, numParents);
			configs = evolveConfigs(winners, numBots, numElites);
		}
	}
	
	private List<MacdTraderConfig> simulateEpoch(final List<MacdTraderConfig> configs, final int epoch) {
		
		// Create a ticker
		
		final BitstampTickerSource tickerNode = new BitstampTickerCsvSource(timestep, interpolationTime, csvPath);
		
		tickerNode.open();
		
		// Create a clock
		
		IClock botClock = new SimulationClock(timestep, simulationStartTime, simulationEndTime, interpolationTime);
		
		final ITradeFloor tradeFloor =  new BitstampSimulationTradeFloor(
				tickerNode.getOutputHigh(),
				tickerNode.getOutputBid(),
				tickerNode.getOutputAsk()
		);
		
		List<SimpleTrendBot> population = createPopulationFromConfigs(configs, tickerNode, tradeFloor, botClock);
		
		HashMap<SimpleTrendBot, DebugTradeLogger> loggers = createLoggers(population);
		
		
		// Run the simulation
		
		botClock.run();
		
		// return population;
		
		
		/* --------------------------------------------------------------------------------------------------------
		 * Ideally you'd just return the bots and let the caller sort the data herself, but since
		 * the bots are now hard wired to the locally defined inputs that wouldn't work. Fix this...
		 * 
		 *  Or at least, we want to couple gene and fitness, so maybe a simple value type:
		 *  
		 *  Score<IConfig> { IConfig config, double profit, int numTrades }
		 *  --------------------------------------------------------------------------------------------------------
		 */
		
		
		// Return the configs sorted by their performance
		
		sortByScore(population, loggers);
		
		List<MacdTraderConfig> sortedConfigs = new ArrayList<MacdTraderConfig>();
		for (SimpleTrendBot bot : population) {
			sortedConfigs.add(bot.getConfig());
		}
		
		LOGGER.debug("Finished epoch " + epoch);
		
		SimpleTrendBot bestBot = population.get(0);
		LOGGER.debug("Best bot was: " + bestBot.getConfig().toString());
		LOGGER.debug("Wallet: " + getBotDollarValue(bestBot));
		LOGGER.debug("Trades: " + loggers.get(bestBot).getActions().size());

		SimpleTrendBot worstBot = population.get(population.size()-1);
		LOGGER.debug("Worst bot was: " + worstBot.getConfig().toString());
		LOGGER.debug("Wallet: " + getBotDollarValue(worstBot));
		LOGGER.debug("Trades: " + loggers.get(worstBot).getActions().size());
		
		LOGGER.debug("...");
		
		
		tickerNode.close();
		
		return sortedConfigs;
	}
	
	private void sortByScore(final List<SimpleTrendBot> population, final HashMap<SimpleTrendBot, DebugTradeLogger> loggers) {
		
		Collections.sort(population, new Comparator<IBot>() {

	        public int compare(IBot botA, IBot botB) {
            	// Move bigger earners to the start, losers to the end
	        	// A minimum # of trade is required, but importance of trade frequency falls off after just a couple of them
	            	
	        	double botAWallet = getBotDollarValue(botA);
	        	double botAActivity = 0.1d + Math.log(loggers.get(botA).getActions().size() * 10000); 
            	double botAPerformance = botAWallet * botAActivity;
            	
            	double botBWallet = getBotDollarValue(botB);
	        	double botBActivity = 0.1d + Math.log(loggers.get(botB).getActions().size() * 10000);
            	double botBPerformance = botBWallet * botBActivity;
            	
            	if (botAPerformance == botBPerformance) {
            		return 0;
            	}
            	
            	return botAPerformance > botBPerformance ? -1 : 1;
	        }
	    });
	}
	
	private double getBotDollarValue(IBot bot) {
		return bot.getTradeFloor().getWalletValueInUsd(bot.getWallet());
	}
	
	private List<SimpleTrendBot> createPopulationFromConfigs(List<MacdTraderConfig> configs, BitstampTickerSource ticker, ITradeFloor tradeFloor, IClock botClock) {
		ArrayList<SimpleTrendBot> population = new ArrayList<SimpleTrendBot>();
		
		double startBtc = walletStartBtcInUsd / ticker.getOutputHigh().getSample(simulationStartTime).value;
		
		for (int i = 0; i < configs.size(); i++) {
			IWallet wallet = new Wallet(walletStartDollars, startBtc);
			SimpleTrendBot bot = new SimpleTrendBot(configs.get(i), timestep, window, ticker.getOutputHigh(), wallet, tradeFloor);
			botClock.addListener(bot);
			population.add(bot);
		}
		
		return population;
	}
	
	private HashMap<SimpleTrendBot, DebugTradeLogger> createLoggers(List<SimpleTrendBot> population) {
		HashMap<SimpleTrendBot, DebugTradeLogger> loggers = new HashMap<SimpleTrendBot, DebugTradeLogger>();
		
		for (SimpleTrendBot bot : population) {
			DebugTradeLogger logger = new DebugTradeLogger();
			bot.addTradeListener(logger);
			loggers.put(bot, logger);
		}
		
		return loggers;
	}
	
	private List<MacdTraderConfig> createRandomConfigs(int numConfigs) {
		ArrayList<MacdTraderConfig> configs = new ArrayList<MacdTraderConfig>();
		
		for (int i = 0; i < numConfigs; i++) {
			MacdTraderConfig config = validateConfig(createRandomConfig());
			configs.add(config);
		}
		
		return configs;
	}
	
	private MacdTraderConfig createRandomConfig() {
			
		MacdTraderConfig config = new MacdTraderConfig(
				getRandomDouble(minDiffThreshold, maxDiffThreshold),
				getRandomDouble(minDiffThreshold, maxDiffThreshold)
		);
		
		return config;
	}
	
	private List<MacdTraderConfig> evolveConfigs(List<MacdTraderConfig> parents, int populationSize, int numElites) {
		List<MacdTraderConfig> newGenes = new ArrayList<MacdTraderConfig>();
		
		int numChildren = populationSize - numElites;
		
		// Crossbreed new children
		for (int i = 0; i < numChildren; i++) {
			MacdTraderConfig parentA = getRandom(parents);
			MacdTraderConfig parentB = getRandom(parents);
			
			MacdTraderConfig child = crossBreedConfig(parentA, parentB);
			child = validateConfig(child);
			newGenes.add(child);
		}
		
		// Add elites to the pool, they survive verbatim
		for (int i = 0; i < numElites; i++) {
			newGenes.add(parents.get(i)); // Assuming the first are the best
		}
		
		return newGenes;
	}
	
	private MacdTraderConfig getRandom(final List<MacdTraderConfig> list) {
		return list.get( (int)(Math.random() * list.size()) );
	}
	
	private MacdTraderConfig crossBreedConfig(final MacdTraderConfig parentA, final MacdTraderConfig parentB) {
		
		// Each property is randomly selected from either parent

		MacdTraderConfig childConfig = new MacdTraderConfig(
				which() ? parentA.minBuyDiffThreshold : parentB.minBuyDiffThreshold,
				which() ? parentA.minSellDiffThreshold : parentB.minSellDiffThreshold);
		
		
		// Mutate
		
		childConfig = mutateConfig(childConfig, mutationChance);
		
		return childConfig;
	}
	
	private MacdTraderConfig mutateConfig(final MacdTraderConfig config, double mutationChance) {
		// Generate a fully random config
		MacdTraderConfig randomConfig = createRandomConfig();
		
		// Each property has a separately evaluated chance of changing to the above generated value
		
		double minBuyThreshold = shouldMutate(0.1d) ? config.minBuyDiffThreshold : config.minBuyDiffThreshold;
		double minSellThreshold = shouldMutate(0.1d) ? config.minSellDiffThreshold : config.minSellDiffThreshold;
		MacdTraderConfig mutatedConfig = new MacdTraderConfig(
				minBuyThreshold,
				minSellThreshold
		);

		
		return mutatedConfig;
	}
	
	private MacdTraderConfig validateConfig(final MacdTraderConfig config) {

///
		
		return config;
	}
	
	
	private boolean which() {
		return shouldMutate(0.5d);
	}
	
	private boolean shouldMutate(double chance) {
		return Math.random() < chance;
	}
	
	private double getRandomDouble(double min, double max) {
		return min + (Math.random() * (max-min));
	}
	
	private long getRandomLong(long min, long max) {
		return min + (long)(Math.random() * (double)(max-min));
	}
}
