package c3po.Training;

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
import c3po.IBotConfig;
import c3po.IClock;
import c3po.ISignal;
import c3po.ITradeFloor;
import c3po.IWallet;
import c3po.SimulationClock;
import c3po.Time;
import c3po.Wallet;
import c3po.bitstamp.BitstampSimulationTickerCsvSource;
import c3po.simulation.SimulationContext;

/**
 * This finds optimal Bot configurations for a given simulation.
 * 
 * 
 * 
 * TODO:
 * 
 * - Trainer history/simulation-context up to current running time to do optimization on. Either:
 * 		- Record it live, or
 * 		- Fetch it from the database (Do this using the new DbSimulationSource)
 * 
 * - Need to define a simulation context
 * 		- Market history ready to go (based on an interface wrapping a recorded source, as described above)
 * 		- Wallet start state
 * 
 * - Need to create bots from this start state
 * 
 * - Need to be able to reset easily reset simulation and run it again with different bots
 * 
 * - Optimize
 * 		- Early out if a convergence threshold is reached
 * 		- Threading
 * 
 * Notes:
 * 
 * - Bot timestep is not affected by genetic modification. It is a constant.
 */

public class GenAlgBotTrainer<TBotConfig extends IBotConfig> implements IBotTrainer<TBotConfig> {	
	private static final Logger LOGGER = LoggerFactory.getLogger(GenAlgBotTrainer.class);
	
	private final GenAlgBotTrainerConfig config;
	private final IBotFactory<TBotConfig> botFactory;
	private final IBotConfigMutator<TBotConfig> mutator;
	private final SimulationContext simContext;
	
	public GenAlgBotTrainer(GenAlgBotTrainerConfig config, IBotConfigMutator<TBotConfig> mutator, IBotFactory<TBotConfig> botFactory, SimulationContext simContext) {
		this.config = config;
		this.botFactory = botFactory;
		this.mutator = mutator;
		this.simContext = simContext;
	}

	public TBotConfig train() {
		List<TBotConfig> configs = createRandomConfigs(config.numBots);
		
		for (int i = 0; i < config.numEpochs; i++) {
			List<IBot<TBotConfig>> population = createPopulationFromConfigs(configs);
			HashMap<IBot<TBotConfig>, DebugTradeLogger> loggers = createLoggers(population);
			
			simulateEpoch(population);
			
			sortByScore(population, loggers);
			
			List<TBotConfig> sortedConfigs = new ArrayList<TBotConfig>();
			for (IBot<TBotConfig> bot : population) {
				sortedConfigs.add(bot.getConfig());
			}
			
			logEpoch(i, population, loggers);
			
			if (i < config.numEpochs-1) { // Leave configs alone at last run
				List<TBotConfig> winners = sortedConfigs.subList(0, config.numParents);
				configs = evolveConfigs(winners, config.numBots, config.numElites);
			}
		}
		
		return configs.get(0);
	}

	private void logEpoch(int i, List<IBot<TBotConfig>> population,
			HashMap<IBot<TBotConfig>, DebugTradeLogger> loggers) {
		LOGGER.debug("Finished epoch " + i);
		
		IBot<TBotConfig> bestBot = population.get(0);
		LOGGER.debug("Best bot was: " + bestBot.getConfig().toString());
		LOGGER.debug("Wallet: " + getBotDollarValue(bestBot));
		LOGGER.debug("Trades: " + loggers.get(bestBot).getActions().size());

		IBot<TBotConfig> worstBot = population.get(population.size()-1);
		LOGGER.debug("Worst bot was: " + worstBot.getConfig().toString());
		LOGGER.debug("Wallet: " + getBotDollarValue(worstBot));
		LOGGER.debug("Trades: " + loggers.get(worstBot).getActions().size());
		
		LOGGER.debug("...");
	}
	
	private void simulateEpoch(final List<IBot<TBotConfig>> population) {
		// Run the simulation
		
		for (IBot<TBotConfig> bot : population) {
			simContext.getClock().addListener(bot);
		}
		
		simContext.getClock().run();
		
		for (IBot<TBotConfig> bot : population) {
			simContext.getClock().removeListener(bot);
		}
		
		simContext.reset();
	}
	
	private void sortByScore(List<IBot<TBotConfig>> population, final HashMap<IBot<TBotConfig>, DebugTradeLogger> loggers) {
		Collections.sort(population, new Comparator<IBot<TBotConfig>>() {

	        public int compare(IBot<TBotConfig> botA, IBot<TBotConfig> botB) {
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
	
	private double getBotDollarValue(final IBot<TBotConfig> bot) {
		return bot.getTradeFloor().getWalletValueInUsd(bot.getWallet());
	}
	
	private List<IBot<TBotConfig>> createPopulationFromConfigs(List<TBotConfig> configs) {
		ArrayList<IBot<TBotConfig>> population = new ArrayList<IBot<TBotConfig>>();

		for (int i = 0; i < configs.size(); i++) {
			IBot<TBotConfig> bot = botFactory.create(configs.get(i));
			population.add(bot);
		}
		
		return population;
	}
	
	private HashMap<IBot<TBotConfig>, DebugTradeLogger> createLoggers(List<IBot<TBotConfig>> population) {
		HashMap<IBot<TBotConfig>, DebugTradeLogger> loggers = new HashMap<IBot<TBotConfig>, DebugTradeLogger>();
		
		for (IBot<TBotConfig> bot : population) {
			DebugTradeLogger logger = new DebugTradeLogger();
			bot.addTradeListener(logger);
			loggers.put(bot, logger);
		}
		
		return loggers;
	}
	
	private List<TBotConfig> createRandomConfigs(int numConfigs) {
		ArrayList<TBotConfig> configs = new ArrayList<TBotConfig>();
		
		for (int i = 0; i < numConfigs; i++) {
			TBotConfig config = mutator.createRandomConfig();
			config = mutator.validateConfig(config);
			configs.add(config);
		}
		
		return configs;
	}
	
	private List<TBotConfig> evolveConfigs(List<TBotConfig> parents, int populationSize, int numElites) {
		List<TBotConfig> newGenes = new ArrayList<TBotConfig>();
		
		int numChildren = populationSize - numElites;
		
		for (int i = 0; i < numChildren; i++) {
			TBotConfig parentA = getRandom(parents);
			TBotConfig parentB = getRandom(parents);
			
			// Crossbreed
			TBotConfig child = mutator.crossBreedConfig(parentA, parentB);
			
			// Mutate
			child = mutator.mutateConfig(child);
			
			// Validate
			child = mutator.validateConfig(child);
			
			newGenes.add(child);
		}
		
		// Add elites to the pool, they survive unharmed
		for (int i = 0; i < numElites; i++) {
			newGenes.add(parents.get(i)); // Assuming the first are the best
		}
		
		return newGenes;
	}
	
	private TBotConfig getRandom(final List<TBotConfig> list) {
		return list.get( (int)(Math.random() * list.size()) );
	}
}
