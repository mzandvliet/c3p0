package c3po.macd;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import c3po.BitstampTickerCsvSource;
import c3po.BitstampTickerSource;
import c3po.BitstampSimulationTradeFloor;
import c3po.IBot;
import c3po.IClock;
import c3po.ISignal;
import c3po.ITradeFloor;
import c3po.Indicators;
import c3po.SimulationClock;

/**
 * This optimizes MacdBots using genetic algoritms.
 * 
 * Todo:
 * 
 * - This would be a lot cleaner with the following network architecture changes
 * 		- Binding input signal dynamically (outside of constructor)
 * 		- Bots having a private wallet but sharing a tradefloor
 * 		- Then you could return bots from simulateEpoch and sort performance externally
 * 
 * - Make IBot<IConfig> with getConfig(), and IBotTrainer<IConfig>
 * 		- This makes genetic algorithm optimization trivial since all you have to do is implement the mutate() function for your IConfig
 * 
 * Notes:
 * 
 * - Bot timestep is not affected by genetic modification
 */

public class MacdBotTrainer {
	private static final Logger LOGGER = LoggerFactory.getLogger(MacdBotTrainer.class);
	
//	private final static String csvPath = "resources/bitstamp_ticker_till_20131117.csv";
//	private final static long simulationStartTime = 1384079023000l;
//	private final static long simulationEndTime = 1384689637000l; 
	
	private final static String csvPath = "resources/bitstamp_ticker_till_20131117_pingpong.csv";
	private final static long simulationStartTime = 1384079023000l;
	private final static long simulationEndTime = 1384682984000l; 
	
	private final static long clockTimestep = 10000;
	private final static long botStepTime = 60000; // Because right now we're keeping it constant, and data sampling rate is ~1 minute
	
	// For a single run of random bots
	
//	private final static int numEpochs = 1;
//	private final static int numBots = 1000;
	
	private final static int numEpochs = 100;
	private final static int numBots = 100;
	
	private final static int numParents = 50;
	private final static int numElites = 5;
	private final static double mutationChance = 0.33d;
	
	private final static double walletStartDollars = 0.0;
	private final static double walletStartBtcInUsd = 1000.0;
	
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
		
		final BitstampTickerSource tickerNode = new BitstampTickerCsvSource(csvPath);
		
		tickerNode.open();
		
		// Create a clock
		
		IClock botClock = new SimulationClock(clockTimestep, simulationStartTime, simulationEndTime);
		List<MacdBot> population = createPopulationFromConfigs(configs, tickerNode, botClock);
		
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
		
		sortByScore(population);
		
		List<MacdBotConfig> sortedConfigs = new ArrayList<MacdBotConfig>();
		for (MacdBot bot : population) {
			sortedConfigs.add(bot.getConfig());
		}
		
		LOGGER.debug("Finished epoch " + epoch);
		
		MacdBot bestBot = population.get(0);
		LOGGER.debug("Best bot was: " + bestBot.getConfig().toString());
		LOGGER.debug("Wallet: " + bestBot.getTradeFloor().getWalletValue());
		
		MacdBot worstBot = population.get(population.size()-1);
		LOGGER.debug("Worst bot was: " + worstBot.getConfig().toString());
		LOGGER.debug("Wallet: " + worstBot.getTradeFloor().getWalletValue());
		
		
		tickerNode.close();
		
		return sortedConfigs;
	}
	
	
	
	private void sortByScore(final List<MacdBot> population) {
		
		Collections.sort(population, new Comparator<IBot>() {

	        public int compare(IBot botA, IBot botB) {
            	// Move bigger earners to the start, losers to the end
	        	// A minimum # of trade is required, but importance of trade frequency falls off after just a couple of them
	            	
	        	double botAWallet = botA.getTradeFloor().getWalletValue();
	        	double botAActivity = 0.1d + Math.log(botA.getTradeFloor().getActions().size()); 
            	double botAPerformance = botAWallet * botAActivity;
            	
            	double botBWallet = botB.getTradeFloor().getWalletValue();
	        	double botBActivity = 0.1d + Math.log(botB.getTradeFloor().getActions().size());
            	double botBPerformance = botBWallet * botBActivity;
            	
            	if (botAPerformance == botBPerformance) {
            		return 0;
            	}
            	
            	return botAPerformance > botBPerformance ? -1 : 1;
	        }
	    });
	}
	
	private List<MacdBot> createPopulationFromConfigs(List<MacdBotConfig> configs, BitstampTickerSource ticker, IClock botClock) {
		ArrayList<MacdBot> population = new ArrayList<MacdBot>();
		
		double startBtc = walletStartBtcInUsd / ticker.getOutputLast().getSample(simulationStartTime).value;
		
		for (int i = 0; i < configs.size(); i++) {
			final ITradeFloor tradeFloor =  new BitstampSimulationTradeFloor(
					ticker.getOutputLast(),
					ticker.getOutputBid(),
					ticker.getOutputAsk(),
					walletStartDollars,
					startBtc
			);
			
			MacdBot bot = new MacdBot(configs.get(i), ticker.getOutputLast(), tradeFloor);
			botClock.addListener(bot);
			population.add(bot);
		}
		
		return population;
	}
	
	private List<MacdBotConfig> createRandomConfigs(int numConfigs) {
		ArrayList<MacdBotConfig> configs = new ArrayList<MacdBotConfig>();
		
		for (int i = 0; i < numConfigs; i++) {
			configs.add(createRandomConfig());
		}
		
		return configs;
	}
	
	private MacdBotConfig createRandomConfig() {		
		MacdAnalysisConfig analysisConfig = new MacdAnalysisConfig(
			getRandomSignalperiod(1000),
			getRandomSignalperiod(1000),
			getRandomSignalperiod(1000)
		);
		
		double minBuyThreshold = 0.0d + (Math.random() * 1.0d);
		double minSellThreshold = -0.5d + (Math.random() * 1.0d);
		MacdTraderConfig traderConfig = new MacdTraderConfig(
				max(analysisConfig),
				minBuyThreshold,
				minSellThreshold,
				Math.random(),
				Math.random(),
				getRandomBackoffDuration(60000l, 86400000l),
				getRandomBackoffDuration(60000l, 86400000l)
				
		);
		
		MacdBotConfig config = new MacdBotConfig(botStepTime, analysisConfig, traderConfig);
		
		return config;
	}
	
	private List<MacdBotConfig> evolveConfigs(List<MacdBotConfig> parents, int populationSize, int numElites) {
		List<MacdBotConfig> newGenes = new ArrayList<MacdBotConfig>();
		
		int numChildren = populationSize - numElites;
		
		// Crossbreed new children
		for (int i = 0; i < numChildren; i++) {
			MacdBotConfig parentA = getRandom(parents);
			MacdBotConfig parentB = getRandom(parents);
			
			MacdBotConfig child = createChild(parentA, parentB);
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
	
	private MacdBotConfig createChild(final MacdBotConfig parentA, final MacdBotConfig parentB) {
		
		// Each property is randomly selected from either parent
		
		MacdAnalysisConfig analysisConfig = new MacdAnalysisConfig(
				which() ? parentA.analysisConfig.slowPeriod : parentB.analysisConfig.slowPeriod,
				which() ? parentA.analysisConfig.fastPeriod : parentB.analysisConfig.fastPeriod,
				which() ? parentA.analysisConfig.signalPeriod : parentB.analysisConfig.signalPeriod);
		
		MacdTraderConfig traderConfig = new MacdTraderConfig(
				max(analysisConfig),
				which() ? parentA.traderConfig.minBuyDiffThreshold : parentB.traderConfig.minBuyDiffThreshold,
				which() ? parentA.traderConfig.minSellDiffThreshold : parentB.traderConfig.minSellDiffThreshold,
				which() ? parentA.traderConfig.usdToBtcTradeAmount : parentB.traderConfig.usdToBtcTradeAmount,
				which() ? parentA.traderConfig.btcToUsdTradeAmount : parentB.traderConfig.btcToUsdTradeAmount,
				which() ? parentA.traderConfig.sellBackoffTimer : parentB.traderConfig.sellBackoffTimer,
				which() ? parentA.traderConfig.buyBackoffTimer : parentB.traderConfig.buyBackoffTimer);
		
		MacdBotConfig childConfig = new MacdBotConfig(
				which() ? parentA.timeStep : parentB.timeStep,
				analysisConfig,
				traderConfig);
		
		// Mutate
		
		childConfig = mutate(childConfig, mutationChance);
		
		return childConfig;
	}
	
	private MacdBotConfig mutate(final MacdBotConfig config, double mutationChance) {
		MacdBotConfig randomConfig = createRandomConfig();
		
		// Each property has a separately evaluated chance of mutating
		
		MacdAnalysisConfig analysisConfig = new MacdAnalysisConfig(
				shouldMutate(mutationChance) ? config.analysisConfig.slowPeriod : randomConfig.analysisConfig.slowPeriod,
				shouldMutate(mutationChance) ? config.analysisConfig.fastPeriod : randomConfig.analysisConfig.fastPeriod,
				shouldMutate(mutationChance) ? config.analysisConfig.signalPeriod : randomConfig.analysisConfig.signalPeriod
			);
			
			double minBuyThreshold = shouldMutate(0.1d) ? config.traderConfig.minBuyDiffThreshold : config.traderConfig.minBuyDiffThreshold;
			double minSellThreshold = shouldMutate(0.1d) ? config.traderConfig.minSellDiffThreshold : config.traderConfig.minSellDiffThreshold;
			MacdTraderConfig traderConfig = new MacdTraderConfig(
					max(analysisConfig),
					minBuyThreshold,
					minSellThreshold,
					shouldMutate(mutationChance) ? config.traderConfig.usdToBtcTradeAmount : config.traderConfig.usdToBtcTradeAmount,
					shouldMutate(mutationChance) ? config.traderConfig.btcToUsdTradeAmount : config.traderConfig.btcToUsdTradeAmount,
					shouldMutate(mutationChance) ? config.traderConfig.sellBackoffTimer : config.traderConfig.sellBackoffTimer,
					shouldMutate(mutationChance) ? config.traderConfig.buyBackoffTimer : config.traderConfig.buyBackoffTimer
			);
			
			MacdBotConfig mutatedConfig = new MacdBotConfig(config.timeStep, analysisConfig, traderConfig);
		
		return mutatedConfig;
	}
	
	private boolean which() {
		return shouldMutate(0.5d);
	}
	
	private boolean shouldMutate(double chance) {
		return Math.random() < chance;
	}
	
	private int getRandomSignalperiod(int range) {
		return 1 + (int) Math.floor(Math.random() * range);
	}
	
	private long getRandomBackoffDuration(long min, long max) {
		return min + (long)(Math.random() * (double)(max-min));
	}
	
	public int max(MacdAnalysisConfig config) {
		return Math.max(config.fastPeriod, Math.max(config.signalPeriod, config.slowPeriod));
	}
}
