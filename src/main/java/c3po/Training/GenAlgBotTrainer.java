package c3po.Training;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import c3po.DebugTradeLogger;
import c3po.IBot;
import c3po.IBotConfig;
import c3po.utils.SignalMath;
import c3po.clock.IClock;
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
	private final IBotConfigMutator<TBotConfig> mutator;
	
	/**
	 * Tracks how many epochs a bot config occured.
	 * Can give an indication whether a setup is weathered or new.
	 */
	private final Map<TBotConfig, Integer> configCounter = new HashMap<TBotConfig, Integer>();
	
	private TBotConfig lastWinner;
	
	public GenAlgBotTrainer(GenAlgBotTrainerConfig config, IBotConfigMutator<TBotConfig> mutator) {
		this.config = config;
		this.mutator = mutator;
	}

	public TBotConfig train(IBotFactory<TBotConfig> botFactory, SimulationContext simContext) {
		// Start with random configs
		List<TBotConfig> configs = createRandomConfigs(config.numBots);
		
		for (int i = 0; i < config.numEpochs; i++) {
			HashMap<TBotConfig, PerformanceResult> results = createResults(configs);
			
			for (int j = 0; j < config.numSimulationsPerEpoch; j++) {
				
				try {
				// Create the population and loggers
				List<IBot<TBotConfig>> population = createPopulationFromConfigs(configs, botFactory);
				HashMap<IBot<TBotConfig>, DebugTradeLogger> loggers = createLoggers(population);
				
				// Simulation range starts at config.simulationLength, and linearly grows towards full simulation range in successive epochs
				// This means first we get feature training, after which we get selection on specific performance
				//long dataRange = config.dataEndTime - config.dataStartTime;
				long simulationTime = config.simulationLength;//SignalMath.getRandomLong(config.simulationLength, SignalMath.lerp(config.simulationLength, dataRange, i / config.numEpochs));
				
				// Set simulation data to some time window between the first and last available data
				long startTime = SignalMath.getRandomLong(config.dataStartTime, config.dataEndTime - simulationTime);
				long endTime = startTime + config.simulationLength;
				simContext.setSimulationRange(startTime, endTime);
				
				// Run the simulation
				simulate(population, simContext);
				
				// Store the results of this run
				for (IBot<TBotConfig> bot : population) {
					PerformanceResult result = results.get(bot.getConfig());
					result.averageNumTrades += loggers.get(bot).getActions().size();
					result.averageWalletValue += getBotDollarValue(bot);
				}
				
				LOGGER.debug("Finished run " + j );
				
				} catch(IllegalStateException e) {
					LOGGER.error("Could not run " + j, e);
				}
			}
			
			// Average the results for this epoch's simulations
			for (TBotConfig config : configs) {
				PerformanceResult result = results.get(config);
				result.averageNumTrades /= this.config.numSimulationsPerEpoch;
				result.averageWalletValue /= this.config.numSimulationsPerEpoch;
				countBotConfig(config);
			}
			
			// Sort
			sortByPerformance(configs, results);
			
			// Log results
			LOGGER.debug("Finished epoch " + i);
			logEpochResults(configs, results);
			
			if (configs.get(0) == lastWinner) {
				LOGGER.debug("Convergence threshold reached!");
				break;
			}
			
			// Evolve the configs (Oh, but leave them alone when we're done)
			if (i < config.numEpochs-1) {
				List<TBotConfig> winners = configs.subList(0, config.numParents);
				configs = evolveConfigs(winners, config.numBots, config.numElites);
			}
		}
		
		LOGGER.debug("Trained bot was present for " + configCounter.get(configs.get(0)) + "/" + config.numEpochs + " epochs");
		return configs.get(0);
	}

	private void logEpochResults(List<TBotConfig> configs, HashMap<TBotConfig, PerformanceResult> results) {
		TBotConfig bestConfig = configs.get(0);
		LOGGER.debug("Best config was: " + bestConfig.toString());
		LOGGER.debug("Average wallet: " + results.get(bestConfig).averageWalletValue);
		LOGGER.debug("Average trades: " + results.get(bestConfig).averageNumTrades);

		TBotConfig worstConfig = configs.get(configs.size()-1);
		LOGGER.debug("Worst config was: " + worstConfig.toString());
		LOGGER.debug("Average wallet: " + results.get(worstConfig).averageWalletValue);
		LOGGER.debug("Average trades: " + results.get(worstConfig).averageNumTrades);
		
		LOGGER.debug("...");
	}
	
	private void simulate(final List<IBot<TBotConfig>> population, SimulationContext simContext) {
		// Run the simulation
		
		IClock clock = simContext.getClock();
		
		for (IBot<TBotConfig> bot : population) {
			clock.addListener(bot);
		}
		
		simContext.run();
		
		for (IBot<TBotConfig> bot : population) {
			clock.removeListener(bot);
		}
	}
	
	private void sortByPerformance(List<TBotConfig> configs, final HashMap<TBotConfig, PerformanceResult> results) {
		Collections.sort(configs, new Comparator<TBotConfig>() {

	        public int compare(TBotConfig configA, TBotConfig configB) {
            	// Move bigger earners to the start, losers to the end
	        	// A minimum # of trade is required, but importance of trade frequency falls off after just a couple of them
	            	
	        	double botAWallet = results.get(configA).averageWalletValue;
	        	double botBWallet = results.get(configB).averageWalletValue;
	        	
	        	double botAActivity = Math.min(results.get(configA).averageNumTrades, config.tradeBias);
	        	double botBActivity = Math.min(results.get(configB).averageNumTrades, config.tradeBias);

            	double configAPerformance = botAWallet * botAActivity;	        	
            	double configBPerformance = botBWallet * botBActivity;
            	
            	if (configAPerformance == configBPerformance) {
            		return 0;
            	}
            	
            	return configAPerformance > configBPerformance ? -1 : 1;
	        }
	    });
	}
	
	private double getBotDollarValue(final IBot<TBotConfig> bot) {
		return bot.getTradeFloor().getWalletValueInUsd(bot.getWallet());
	}
	
	private HashMap<TBotConfig, PerformanceResult> createResults(List<TBotConfig> configs) {
		HashMap<TBotConfig, PerformanceResult> results = new HashMap<TBotConfig, PerformanceResult>();
		
		for (TBotConfig config : configs) {
			results.put(config, new PerformanceResult());
		}
		
		return results;
	}
	
	private List<IBot<TBotConfig>> createPopulationFromConfigs(List<TBotConfig> configs, IBotFactory<TBotConfig> botFactory) {
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
			TBotConfig parentA = getRandomConfig(parents);
			TBotConfig parentB = getRandomConfig(parents);
			
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
	
	private TBotConfig getRandomConfig(final List<TBotConfig> list) {
		return list.get( (int)(Math.random() * list.size()) );
	}
	
	private void countBotConfig(TBotConfig config) {
		if(!configCounter.containsKey(config)) {
			configCounter.put(config, 1);
		} else {
			configCounter.put(config, configCounter.get(config) + 1);
		}
	}
	
	private class PerformanceResult {
		public int averageNumTrades;
		public double averageWalletValue;
	}
}
