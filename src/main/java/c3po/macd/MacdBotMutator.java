package c3po.macd;

import c3po.*;
import c3po.Training.*;

public class MacdBotMutator implements IBotConfigMutator<MacdBotConfig> {
	private final MacdBotMutatorConfig mutatorConfig;
	private final long timestep;
	
	public MacdBotMutator(MacdBotMutatorConfig config) {
		this.mutatorConfig = config;
		this.timestep = 1 * Time.MINUTES;
	}

	@Override
	public MacdBotConfig createRandomConfig() {

		final MacdAnalysisConfig buyAnalysisConfig = new MacdAnalysisConfig(
				getRandomLong(mutatorConfig.minAnalysisPeriod, mutatorConfig.maxAnalysisPeriod),
				getRandomLong(mutatorConfig.minAnalysisPeriod, mutatorConfig.maxAnalysisPeriod),
				getRandomLong(mutatorConfig.minAnalysisPeriod, mutatorConfig.maxAnalysisPeriod)
		);
		
		final MacdAnalysisConfig sellAnalysisConfig = new MacdAnalysisConfig(
				getRandomLong(mutatorConfig.minAnalysisPeriod, mutatorConfig.maxAnalysisPeriod),
				getRandomLong(mutatorConfig.minAnalysisPeriod, mutatorConfig.maxAnalysisPeriod),
				getRandomLong(mutatorConfig.minAnalysisPeriod, mutatorConfig.maxAnalysisPeriod)
		);
		
		final MacdTraderConfig traderConfig = new MacdTraderConfig(
				getRandomDouble(mutatorConfig.minBuyDiffThreshold, mutatorConfig.maxBuyDiffThreshold),
				getRandomDouble(mutatorConfig.minSellDiffThreshold, mutatorConfig.maxSellDiffThreshold),
				getRandomDouble(mutatorConfig.minLossCuttingPercentage, mutatorConfig.maxLossCuttingPercentage)
		);
		
		final MacdBotConfig config = new MacdBotConfig(timestep, buyAnalysisConfig, sellAnalysisConfig, traderConfig);
		
		return config;
	}
	
	@Override
	public MacdBotConfig crossBreedConfig(final MacdBotConfig parentA, final MacdBotConfig parentB) {
		
		// Each property is randomly selected from either parent
		
		final MacdAnalysisConfig buyAnalysisConfig = new MacdAnalysisConfig(
				which() ? parentA.buyAnalysisConfig.fastPeriod : parentB.buyAnalysisConfig.fastPeriod,
				which() ? parentA.buyAnalysisConfig.slowPeriod : parentB.buyAnalysisConfig.slowPeriod,
				which() ? parentA.buyAnalysisConfig.signalPeriod : parentB.buyAnalysisConfig.signalPeriod
		);
		
		final MacdAnalysisConfig sellAnalysisConfig = new MacdAnalysisConfig(
				which() ? parentA.sellAnalysisConfig.fastPeriod : parentB.sellAnalysisConfig.fastPeriod,
				which() ? parentA.sellAnalysisConfig.slowPeriod : parentB.sellAnalysisConfig.slowPeriod,
				which() ? parentA.sellAnalysisConfig.signalPeriod : parentB.sellAnalysisConfig.signalPeriod
		);
		
		final MacdTraderConfig traderConfig = new MacdTraderConfig(
				which() ? parentA.traderConfig.minBuyDiffThreshold : parentB.traderConfig.minBuyDiffThreshold,
				which() ? parentA.traderConfig.minSellDiffThreshold : parentB.traderConfig.minSellDiffThreshold,
				which() ? parentA.traderConfig.lossCutThreshold : parentB.traderConfig.lossCutThreshold
													
		);
		
		final MacdBotConfig childConfig = new MacdBotConfig(
				which() ? parentA.timestep : parentB.timestep,
				buyAnalysisConfig, sellAnalysisConfig,
				traderConfig);
		
		return childConfig;
	}
	
	@Override
	public MacdBotConfig mutateConfig(final MacdBotConfig config) {
		// Generate a fully random config
		final MacdBotConfig randomConfig = createRandomConfig();
		
		// Each property has a separately evaluated chance of changing to the above generated value
		
		final MacdAnalysisConfig buyAnalysisConfig = new MacdAnalysisConfig(
				shouldMutate(mutatorConfig.mutationChance) ? randomConfig.buyAnalysisConfig.fastPeriod : config.buyAnalysisConfig.fastPeriod,
				shouldMutate(mutatorConfig.mutationChance) ? randomConfig.buyAnalysisConfig.slowPeriod : config.buyAnalysisConfig.slowPeriod,
				shouldMutate(mutatorConfig.mutationChance) ? randomConfig.buyAnalysisConfig.signalPeriod : config.buyAnalysisConfig.signalPeriod
		);
		
		final MacdAnalysisConfig sellAnalysisConfig = new MacdAnalysisConfig(
				shouldMutate(mutatorConfig.mutationChance) ? randomConfig.sellAnalysisConfig.fastPeriod : config.sellAnalysisConfig.fastPeriod,
				shouldMutate(mutatorConfig.mutationChance) ? randomConfig.sellAnalysisConfig.slowPeriod : config.sellAnalysisConfig.slowPeriod,
				shouldMutate(mutatorConfig.mutationChance) ? randomConfig.sellAnalysisConfig.signalPeriod : config.sellAnalysisConfig.signalPeriod
		);
			
		final MacdTraderConfig traderConfig = new MacdTraderConfig(
				shouldMutate(mutatorConfig.mutationChance) ? randomConfig.traderConfig.minBuyDiffThreshold : config.traderConfig.minBuyDiffThreshold,
				shouldMutate(mutatorConfig.mutationChance) ? randomConfig.traderConfig.minSellDiffThreshold : config.traderConfig.minSellDiffThreshold,
				shouldMutate(mutatorConfig.mutationChance) ? randomConfig.traderConfig.lossCutThreshold : config.traderConfig.lossCutThreshold
		);
		
		final MacdBotConfig mutatedConfig = new MacdBotConfig(config.timestep, buyAnalysisConfig, sellAnalysisConfig, traderConfig);
		
		return mutatedConfig;
	}
	
	@Override
	public MacdBotConfig validateConfig(final MacdBotConfig config) {
		/*
		 *  Ensures some basic common sense. The genetic algorithm loves to get stuck on an otherwise insane config that just happens to fit the data.
		 */
		
		final MacdAnalysisConfig validBuyAnalysisConfig = new MacdAnalysisConfig(
				config.buyAnalysisConfig.fastPeriod  > config.buyAnalysisConfig.slowPeriod ?
						getRandomLong(1 * Time.MINUTES, config.buyAnalysisConfig.slowPeriod) :
						config.buyAnalysisConfig.fastPeriod,
				config.buyAnalysisConfig.slowPeriod,
				config.buyAnalysisConfig.signalPeriod
		);
		
		final MacdAnalysisConfig validSellAnalysisConfig = new MacdAnalysisConfig(
				config.sellAnalysisConfig.fastPeriod  > config.sellAnalysisConfig.slowPeriod ?
						getRandomLong(1 * Time.MINUTES, config.sellAnalysisConfig.slowPeriod) :
						config.sellAnalysisConfig.fastPeriod,
				config.sellAnalysisConfig.slowPeriod,
				config.sellAnalysisConfig.signalPeriod
		);
			
		final MacdBotConfig validConfig = new MacdBotConfig(config.timestep, validBuyAnalysisConfig, validSellAnalysisConfig, config.traderConfig);
		
		return validConfig;
	}
	
	protected boolean which() {
		return shouldMutate(0.5d);
	}
	
	protected boolean shouldMutate(double chance) {
		return Math.random() < chance;
	}
	
	protected double getRandomDouble(double min, double max) {
		return min + (Math.random() * (max-min));
	}
	
	protected long getRandomLong(long min, long max) {
		return min + (long)(Math.random() * (double)(max-min));
	}
}
