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
		
		final long fast = getRandomLong(mutatorConfig.minAnalysisPeriod, mutatorConfig.maxAnalysisPeriod);
		final long slow = getRandomLong(mutatorConfig.minAnalysisPeriod, mutatorConfig.maxAnalysisPeriod);
		final long signal = getRandomLong(mutatorConfig.minAnalysisPeriod, mutatorConfig.maxAnalysisPeriod);
		
		final MacdAnalysisConfig analysisConfig = new MacdAnalysisConfig(
			fast,
			slow,
			signal
		);
		
		final MacdTraderConfig traderConfig = new MacdTraderConfig(
				getRandomDouble(mutatorConfig.minBuyDiffThreshold, mutatorConfig.maxBuyDiffThreshold),
				getRandomDouble(mutatorConfig.minSellDiffThreshold, mutatorConfig.maxSellDiffThreshold),
				getRandomDouble(mutatorConfig.minBuyPercentage, mutatorConfig.maxBuyPercentage),
				getRandomDouble(mutatorConfig.minSellPercentage, mutatorConfig.maxSellPercentage),
				getRandomLong(mutatorConfig.minBuyBackoffTimer, mutatorConfig.maxBuyBackoffTimer),
				getRandomLong(mutatorConfig.minSellBackoffTimer, mutatorConfig.maxSellBackoffTimer),
				getRandomDouble(mutatorConfig.minLossCuttingPercentage, mutatorConfig.maxLossCuttingPercentage)
		);
		
		final MacdBotConfig config = new MacdBotConfig(timestep, analysisConfig, traderConfig);
		
		return config;
	}
	
	@Override
	public MacdBotConfig crossBreedConfig(final MacdBotConfig parentA, final MacdBotConfig parentB) {
		
		// Each property is randomly selected from either parent
		
		final MacdAnalysisConfig analysisConfig = new MacdAnalysisConfig(
				which() ? parentA.analysisConfig.fastPeriod : parentB.analysisConfig.fastPeriod,
				which() ? parentA.analysisConfig.slowPeriod : parentB.analysisConfig.slowPeriod,
				which() ? parentA.analysisConfig.signalPeriod : parentB.analysisConfig.signalPeriod
		);
		
		final MacdTraderConfig traderConfig = new MacdTraderConfig(
				which() ? parentA.traderConfig.minBuyDiffThreshold : parentB.traderConfig.minBuyDiffThreshold,
				which() ? parentA.traderConfig.minSellDiffThreshold : parentB.traderConfig.minSellDiffThreshold,
				which() ? parentA.traderConfig.buyPercentage : parentB.traderConfig.buyPercentage,
				which() ? parentA.traderConfig.sellPercentage : parentB.traderConfig.sellPercentage,
				which() ? parentA.traderConfig.buyBackoffTimer : parentB.traderConfig.buyBackoffTimer,
				which() ? parentA.traderConfig.sellBackoffTimer : parentB.traderConfig.sellBackoffTimer,
				which() ? parentA.traderConfig.lossCuttingPercentage : parentB.traderConfig.lossCuttingPercentage
													
		);
		
		final MacdBotConfig childConfig = new MacdBotConfig(
				which() ? parentA.timestep : parentB.timestep,
				analysisConfig,
				traderConfig);
		
		return childConfig;
	}
	
	@Override
	public MacdBotConfig mutateConfig(final MacdBotConfig config) {
		// Generate a fully random config
		final MacdBotConfig randomConfig = createRandomConfig();
		
		// Each property has a separately evaluated chance of changing to the above generated value
		
		final MacdAnalysisConfig analysisConfig = new MacdAnalysisConfig(
				shouldMutate(mutatorConfig.mutationChance) ? randomConfig.analysisConfig.fastPeriod : config.analysisConfig.fastPeriod,
				shouldMutate(mutatorConfig.mutationChance) ? randomConfig.analysisConfig.slowPeriod : config.analysisConfig.slowPeriod,
				shouldMutate(mutatorConfig.mutationChance) ? randomConfig.analysisConfig.signalPeriod : config.analysisConfig.signalPeriod
		);
			
		final MacdTraderConfig traderConfig = new MacdTraderConfig(
				shouldMutate(mutatorConfig.mutationChance) ? randomConfig.traderConfig.minBuyDiffThreshold : config.traderConfig.minBuyDiffThreshold,
				shouldMutate(mutatorConfig.mutationChance) ? randomConfig.traderConfig.minSellDiffThreshold : config.traderConfig.minSellDiffThreshold,
				shouldMutate(mutatorConfig.mutationChance) ? randomConfig.traderConfig.buyPercentage : config.traderConfig.buyPercentage,
				shouldMutate(mutatorConfig.mutationChance) ? randomConfig.traderConfig.sellPercentage : config.traderConfig.sellPercentage,
				shouldMutate(mutatorConfig.mutationChance) ? randomConfig.traderConfig.buyBackoffTimer : config.traderConfig.buyBackoffTimer,
				shouldMutate(mutatorConfig.mutationChance) ? randomConfig.traderConfig.sellBackoffTimer : config.traderConfig.sellBackoffTimer,
				shouldMutate(mutatorConfig.mutationChance) ? randomConfig.traderConfig.lossCuttingPercentage : config.traderConfig.lossCuttingPercentage
		);
		
		final MacdBotConfig mutatedConfig = new MacdBotConfig(config.timestep, analysisConfig, traderConfig);
		
		return mutatedConfig;
	}
	
	@Override
	public MacdBotConfig validateConfig(final MacdBotConfig config) {
		/*
		 *  Ensures some basic common sense. The genetic algorithm loves to get stuck on an otherwise insane config that just happens to fit the data.
		 */
		
		final MacdAnalysisConfig validAnalysisConfig = new MacdAnalysisConfig(
				config.analysisConfig.fastPeriod  > config.analysisConfig.slowPeriod ?
						getRandomLong(1 * Time.MINUTES, config.analysisConfig.slowPeriod) :
						config.analysisConfig.fastPeriod,
				config.analysisConfig.slowPeriod,
				config.analysisConfig.signalPeriod
		);
			
		final MacdBotConfig validConfig = new MacdBotConfig(config.timestep, validAnalysisConfig, config.traderConfig);
		
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
