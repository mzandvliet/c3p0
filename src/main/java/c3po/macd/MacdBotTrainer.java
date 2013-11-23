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
 * - Bots luuuuurrve to hold way too long-term positions, make daytraders. (Build risk into macd with volatility node)
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

public class MacdBotTrainer {
	private static final Logger LOGGER = LoggerFactory.getLogger(MacdBotTrainer.class);
	
	private final static String csvPath = "resources/bitstamp_ticker_till_20131122_crashed.csv";
	private final static long simulationStartTime = 1384079023000l;
	private final static long simulationEndTime = 1385192429000l; 
	
//	private final static String csvPath = "resources/bitstamp_ticker_till_20131117_pingpong.csv";
//	private final static long simulationStartTime = 1384079023000l;
//	private final static long simulationEndTime = 1384682984000l; 
	
	private final static long timestep = 60000; // Because right now we're keeping it constant, and data sampling rate is ~1 minute
	
	private final static long interpolationTime = 120000;
	
	// For a single run of random bots
	
//	private final static int numEpochs = 1;
//	private final static int numBots = 1000;
	
	private final static int numEpochs = 100;
	private final static int numBots = 100;
	
	private final static int numParents = 50;
	private final static int numElites = 5;
	private final static double mutationChance = 0.33d;
	
	private final static double walletStartDollars = 500.0;
	private final static double walletStartBtcInUsd = 500.0;
	
	public static void main(String[] args) throws ClassNotFoundException, SQLException {
		MacdBotTrainer trainer = new MacdBotTrainer();
		trainer.run();		
	}
	
	public void run() {
		List<MacdBotConfig> configs = createRandomConfigs(numBots);
		
		for (int i = 0; i < numEpochs; i++) {
			List<MacdBotConfig> sortedConfigs = simulateEpoch(configs, i);
			List<MacdBotConfig> winners = sortedConfigs.subList(0, numParents);
			configs = evolveConfigs(winners, numBots, numElites);
		}
	}
	
	private List<MacdBotConfig> simulateEpoch(final List<MacdBotConfig> configs, final int epoch) {
		
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
		
		List<MacdBot> population = createPopulationFromConfigs(configs, tickerNode, tradeFloor, botClock);
		
		HashMap<MacdBot, DebugTradeLogger> loggers = createLoggers(population);
		
		
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
		
		List<MacdBotConfig> sortedConfigs = new ArrayList<MacdBotConfig>();
		for (MacdBot bot : population) {
			sortedConfigs.add(bot.getConfig());
		}
		
		LOGGER.debug("Finished epoch " + epoch);
		
		MacdBot bestBot = population.get(0);
		LOGGER.debug("Best bot was: " + bestBot.getConfig().toString());
		LOGGER.debug("Wallet: " + bestBot.getTradeFloor().getWalletValueInUsd(bestBot.getWallet()));
		LOGGER.debug("Trades: " + loggers.get(bestBot).getActions().size());

		MacdBot worstBot = population.get(population.size()-1);
		LOGGER.debug("Worst bot was: " + worstBot.getConfig().toString());
		LOGGER.debug("Wallet: " + worstBot.getTradeFloor().getWalletValueInUsd(worstBot.getWallet()));
		LOGGER.debug("Trades: " + loggers.get(worstBot).getActions().size());
		
		LOGGER.debug("...");
		
		
		tickerNode.close();
		
		return sortedConfigs;
	}
	
	private void sortByScore(final List<MacdBot> population, final HashMap<MacdBot, DebugTradeLogger> loggers) {
		
		Collections.sort(population, new Comparator<IBot>() {

	        public int compare(IBot botA, IBot botB) {
            	// Move bigger earners to the start, losers to the end
	        	// A minimum # of trade is required, but importance of trade frequency falls off after just a couple of them
	            	
	        	double botAWallet = botA.getTradeFloor().getWalletValueInUsd(botA.getWallet());
	        	double botAActivity = 0.1d + Math.log(loggers.get(botA).getActions().size() * 10000); 
            	double botAPerformance = botAWallet * botAActivity;
            	
            	double botBWallet = botB.getTradeFloor().getWalletValueInUsd(botB.getWallet());
	        	double botBActivity = 0.1d + Math.log(loggers.get(botB).getActions().size() * 10000);
            	double botBPerformance = botBWallet * botBActivity;
            	
            	if (botAPerformance == botBPerformance) {
            		return 0;
            	}
            	
            	return botAPerformance > botBPerformance ? -1 : 1;
	        }
	    });
	}
	
	private List<MacdBot> createPopulationFromConfigs(List<MacdBotConfig> configs, BitstampTickerSource ticker, ITradeFloor tradeFloor, IClock botClock) {
		ArrayList<MacdBot> population = new ArrayList<MacdBot>();
		
		double startBtc = walletStartBtcInUsd / ticker.getOutputHigh().getSample(simulationStartTime).value;
		
		for (int i = 0; i < configs.size(); i++) {
			IWallet wallet = new Wallet(walletStartDollars, startBtc);
			MacdBot bot = new MacdBot(configs.get(i), ticker.getOutputHigh(), wallet, tradeFloor);
			botClock.addListener(bot);
			population.add(bot);
		}
		
		return population;
	}
	
	private HashMap<MacdBot, DebugTradeLogger> createLoggers(List<MacdBot> population) {
		HashMap<MacdBot, DebugTradeLogger> loggers = new HashMap<MacdBot, DebugTradeLogger>();
		
		for (MacdBot bot : population) {
			DebugTradeLogger logger = new DebugTradeLogger();
			bot.addTradeListener(logger);
			loggers.put(bot, logger);
		}
		
		return loggers;
	}
	
	private List<MacdBotConfig> createRandomConfigs(int numConfigs) {
		ArrayList<MacdBotConfig> configs = new ArrayList<MacdBotConfig>();
		
		for (int i = 0; i < numConfigs; i++) {
			MacdBotConfig config = validateConfig(createRandomConfig());
			configs.add(config);
		}
		
		return configs;
	}
	
	private MacdBotConfig createRandomConfig() {
		
		long fast = getRandomSignalperiod(1 * Time.MINUTES, 1000 * Time.MINUTES);
		long slow = getRandomSignalperiod(1 * Time.MINUTES, 1000 * Time.MINUTES);
		long signal = getRandomSignalperiod(1 * Time.MINUTES, 1000 * Time.MINUTES);
		MacdAnalysisConfig analysisConfig = new MacdAnalysisConfig(
			fast,
			slow,
			signal
		);
		
		double minBuyThreshold = 0.0d + (Math.random() * 1.0d);
		double minSellThreshold = -0.5d + (Math.random() * 1.0d);
		MacdTraderConfig traderConfig = new MacdTraderConfig(
				minBuyThreshold,
				minSellThreshold,
				Math.random(),
				Math.random(),
				getRandomBackoffDuration(60000l, 86400000l),
				getRandomBackoffDuration(60000l, 86400000l)
				
		);
		
		MacdBotConfig config = new MacdBotConfig(timestep, analysisConfig, traderConfig);
		
		return config;
	}
	
	private List<MacdBotConfig> evolveConfigs(List<MacdBotConfig> parents, int populationSize, int numElites) {
		List<MacdBotConfig> newGenes = new ArrayList<MacdBotConfig>();
		
		int numChildren = populationSize - numElites;
		
		// Crossbreed new children
		for (int i = 0; i < numChildren; i++) {
			MacdBotConfig parentA = getRandom(parents);
			MacdBotConfig parentB = getRandom(parents);
			
			MacdBotConfig child = crossBreedConfig(parentA, parentB);
			child = validateConfig(child);
			newGenes.add(child);
		}
		
		// Add elites to the pool, they survive verbatim
		for (int i = 0; i < numElites; i++) {
			newGenes.add(parents.get(i)); // Assuming the first are the best
		}
		
		return newGenes;
	}
	
	private MacdBotConfig getRandom(final List<MacdBotConfig> list) {
		return list.get( (int)(Math.random() * list.size()) );
	}
	
	private MacdBotConfig crossBreedConfig(final MacdBotConfig parentA, final MacdBotConfig parentB) {
		
		// Each property is randomly selected from either parent
		
		MacdAnalysisConfig analysisConfig = new MacdAnalysisConfig(
				which() ? parentA.analysisConfig.fastPeriod : parentB.analysisConfig.fastPeriod,
				which() ? parentA.analysisConfig.slowPeriod : parentB.analysisConfig.slowPeriod,
				which() ? parentA.analysisConfig.signalPeriod : parentB.analysisConfig.signalPeriod);
		
		MacdTraderConfig traderConfig = new MacdTraderConfig(
				which() ? parentA.traderConfig.minBuyDiffThreshold : parentB.traderConfig.minBuyDiffThreshold,
				which() ? parentA.traderConfig.minSellDiffThreshold : parentB.traderConfig.minSellDiffThreshold,
				which() ? parentA.traderConfig.usdToBtcTradeAmount : parentB.traderConfig.usdToBtcTradeAmount,
				which() ? parentA.traderConfig.btcToUsdTradeAmount : parentB.traderConfig.btcToUsdTradeAmount,
				which() ? parentA.traderConfig.sellBackoffTimer : parentB.traderConfig.sellBackoffTimer,
				which() ? parentA.traderConfig.buyBackoffTimer : parentB.traderConfig.buyBackoffTimer);
		
		MacdBotConfig childConfig = new MacdBotConfig(
				which() ? parentA.timestep : parentB.timestep,
				analysisConfig,
				traderConfig);
		
		// Mutate
		
		childConfig = mutateConfig(childConfig, mutationChance);
		
		return childConfig;
	}
	
	private MacdBotConfig mutateConfig(final MacdBotConfig config, double mutationChance) {
		MacdBotConfig randomConfig = createRandomConfig();
		
		// Each property has a separately evaluated chance of mutating
		
		MacdAnalysisConfig analysisConfig = new MacdAnalysisConfig(
				shouldMutate(mutationChance) ? config.analysisConfig.fastPeriod : randomConfig.analysisConfig.fastPeriod,
				shouldMutate(mutationChance) ? config.analysisConfig.slowPeriod : randomConfig.analysisConfig.slowPeriod,
				shouldMutate(mutationChance) ? config.analysisConfig.signalPeriod : randomConfig.analysisConfig.signalPeriod
			);
			
		double minBuyThreshold = shouldMutate(0.1d) ? config.traderConfig.minBuyDiffThreshold : config.traderConfig.minBuyDiffThreshold;
		double minSellThreshold = shouldMutate(0.1d) ? config.traderConfig.minSellDiffThreshold : config.traderConfig.minSellDiffThreshold;
		MacdTraderConfig traderConfig = new MacdTraderConfig(
				minBuyThreshold,
				minSellThreshold,
				shouldMutate(mutationChance) ? config.traderConfig.usdToBtcTradeAmount : config.traderConfig.usdToBtcTradeAmount,
				shouldMutate(mutationChance) ? config.traderConfig.btcToUsdTradeAmount : config.traderConfig.btcToUsdTradeAmount,
				shouldMutate(mutationChance) ? config.traderConfig.sellBackoffTimer : config.traderConfig.sellBackoffTimer,
				shouldMutate(mutationChance) ? config.traderConfig.buyBackoffTimer : config.traderConfig.buyBackoffTimer
		);
		
		MacdBotConfig mutatedConfig = new MacdBotConfig(config.timestep, analysisConfig, traderConfig);
		
		return mutatedConfig;
	}
	
	private MacdBotConfig validateConfig(final MacdBotConfig config) {
		/*
		 *  Ensures some basic common sense. The genetic algorithm loves to get stuck on an otherwise insane config that just happens to fit the data.
		 */
		
		MacdAnalysisConfig validAnalysisConfig = new MacdAnalysisConfig(
				config.analysisConfig.fastPeriod  > config.analysisConfig.slowPeriod ?
						getRandomSignalperiod(1 * Time.MINUTES, config.analysisConfig.slowPeriod) :
						config.analysisConfig.fastPeriod,
				config.analysisConfig.slowPeriod,
				config.analysisConfig.signalPeriod
			);
			
		MacdBotConfig validConfig = new MacdBotConfig(config.timestep, validAnalysisConfig, config.traderConfig);
		
		return validConfig;
	}
	
	
	private boolean which() {
		return shouldMutate(0.5d);
	}
	
	private boolean shouldMutate(double chance) {
		return Math.random() < chance;
	}
	
	private long getRandomSignalperiod(long min, long max) {
		return min + (long)(Math.random() * (double)(max-min));
	}
	
	private long getRandomBackoffDuration(long min, long max) {
		return min + (long)(Math.random() * (double)(max-min));
	}
}
