package c3po.macd;

import c3po.Training.*;
import c3po.utils.SignalMath;
import c3po.utils.Time;

/* TODO: Refactor to avoid copy pasta. Mutating analysisconfigs for example. */

public class MacdBotMutator implements IBotConfigMutator<MacdBotConfig> {
	private final MacdBotMutatorConfig mutatorConfig;
	private final long timestep;
	
	public MacdBotMutator(MacdBotMutatorConfig config) {
		this.mutatorConfig = config;
		this.timestep = 1 * Time.MINUTES;
	}

	@Override
	public MacdBotConfig createRandomConfig() {

		final MacdAnalysisConfig buyAnalysisConfig = createRandomAnalysisConfig(mutatorConfig.minAnalysisPeriod, mutatorConfig.maxAnalysisPeriod);
		final MacdAnalysisConfig sellAnalysisConfig = createRandomAnalysisConfig(mutatorConfig.minAnalysisPeriod, mutatorConfig.maxAnalysisPeriod);
		final MacdAnalysisConfig volumeAnalysisConfig = createRandomAnalysisConfig(mutatorConfig.minAnalysisPeriod, mutatorConfig.maxAnalysisPeriod);

		final MacdTraderConfig traderConfig = createRandomTraderConfig(); 
		
		final MacdBotConfig config = new MacdBotConfig(timestep, buyAnalysisConfig, sellAnalysisConfig, volumeAnalysisConfig, traderConfig);
		
		return config;
	}
	
	private MacdAnalysisConfig createRandomAnalysisConfig(long minAnalysisPeriod, long maxAnalysisPeriod) {
		return new MacdAnalysisConfig (
				SignalMath.getRandomLong(minAnalysisPeriod, maxAnalysisPeriod),
				SignalMath.getRandomLong(minAnalysisPeriod, maxAnalysisPeriod),
				SignalMath.getRandomLong(minAnalysisPeriod, maxAnalysisPeriod)
		);
	}
	
	private MacdTraderConfig createRandomTraderConfig() {
		return new MacdTraderConfig(
				SignalMath.getRandomDouble(mutatorConfig.minBuyDiffThreshold, mutatorConfig.maxBuyDiffThreshold),
				SignalMath.getRandomDouble(mutatorConfig.minSellDiffThreshold, mutatorConfig.maxSellDiffThreshold),
				SignalMath.getRandomDouble(mutatorConfig.minBuyVolumeThreshold, mutatorConfig.maxBuyVolumeThreshold),
				SignalMath.getRandomLong(mutatorConfig.minSellPricePeriod, mutatorConfig.maxSellPricePeriod),
				SignalMath.getRandomDouble(mutatorConfig.minLossCuttingPercentage, mutatorConfig.maxLossCuttingPercentage),
				SignalMath.getRandomDouble(mutatorConfig.minSellThresholdRelaxationFactor, mutatorConfig.maxSellThresholdRelaxationFactor)
		);
	}
	
	@Override
	public MacdBotConfig crossBreedConfig(final MacdBotConfig parentA, final MacdBotConfig parentB) {
		
		// Each property is randomly selected from either parent
		
		final MacdAnalysisConfig buyAnalysisConfig = crossBreedNewAnalysisConfig(parentA.buyAnalysisConfig, parentB.buyAnalysisConfig);
		final MacdAnalysisConfig sellAnalysisConfig = crossBreedNewAnalysisConfig(parentA.sellAnalysisConfig, parentB.sellAnalysisConfig);
		final MacdAnalysisConfig volumeAnalysisConfig = crossBreedNewAnalysisConfig(parentA.volumeAnalysisConfig, parentB.volumeAnalysisConfig);
		
		final MacdTraderConfig traderConfig = new MacdTraderConfig(
				which() ? parentA.traderConfig.minBuyDiffThreshold : parentB.traderConfig.minBuyDiffThreshold,
				which() ? parentA.traderConfig.minSellDiffThreshold : parentB.traderConfig.minSellDiffThreshold,
				which() ? parentA.traderConfig.buyVolumeThreshold : parentB.traderConfig.buyVolumeThreshold,
				which() ? parentA.traderConfig.sellPricePeriod : parentB.traderConfig.sellPricePeriod,
				which() ? parentA.traderConfig.lossCutThreshold : parentB.traderConfig.lossCutThreshold,
				which() ? parentA.traderConfig.sellThresholdRelaxationFactor : parentB.traderConfig.sellThresholdRelaxationFactor
		);
		
		final MacdBotConfig childConfig = new MacdBotConfig(
				which() ? parentA.timestep : parentB.timestep,
				buyAnalysisConfig,
				sellAnalysisConfig,
				volumeAnalysisConfig,
				traderConfig);
		
		return childConfig;
	}
	
	private MacdAnalysisConfig crossBreedNewAnalysisConfig(MacdAnalysisConfig parentA, MacdAnalysisConfig parentB) {
		return new MacdAnalysisConfig(
				which() ? parentA.fastPeriod : parentB.fastPeriod,
				which() ? parentA.slowPeriod : parentB.slowPeriod,
				which() ? parentA.signalPeriod : parentB.signalPeriod
		);
	}
	
	@Override
	public MacdBotConfig mutateConfig(final MacdBotConfig config) {		
		// Each property has a separately evaluated chance of changing to the above generated value
		
		final MacdAnalysisConfig buyAnalysisConfig = mutateAnalysisConfig(config.buyAnalysisConfig);
		final MacdAnalysisConfig sellAnalysisConfig = mutateAnalysisConfig(config.sellAnalysisConfig);
		final MacdAnalysisConfig volumeAnalysisConfig = mutateAnalysisConfig(config.volumeAnalysisConfig);
			
		final MacdTraderConfig randomTraderConfig = createRandomTraderConfig();
		
		final MacdTraderConfig traderConfig = new MacdTraderConfig(
				shouldMutate(mutatorConfig.mutationChance) ? randomTraderConfig.minBuyDiffThreshold : config.traderConfig.minBuyDiffThreshold,
				shouldMutate(mutatorConfig.mutationChance) ? randomTraderConfig.minSellDiffThreshold : config.traderConfig.minSellDiffThreshold,
				shouldMutate(mutatorConfig.mutationChance) ? randomTraderConfig.buyVolumeThreshold : config.traderConfig.buyVolumeThreshold,
				shouldMutate(mutatorConfig.mutationChance) ? randomTraderConfig.sellPricePeriod : config.traderConfig.sellPricePeriod,
				shouldMutate(mutatorConfig.mutationChance) ? randomTraderConfig.lossCutThreshold : config.traderConfig.lossCutThreshold,
				shouldMutate(mutatorConfig.mutationChance) ? randomTraderConfig.sellThresholdRelaxationFactor : config.traderConfig.sellThresholdRelaxationFactor
		);
		
		final MacdBotConfig mutatedConfig = new MacdBotConfig(config.timestep, buyAnalysisConfig, sellAnalysisConfig, volumeAnalysisConfig, traderConfig);
		
		return mutatedConfig;
	}
	
	private MacdAnalysisConfig mutateAnalysisConfig(final MacdAnalysisConfig config) {
		MacdAnalysisConfig randomConfig = createRandomAnalysisConfig(mutatorConfig.minAnalysisPeriod, mutatorConfig.maxAnalysisPeriod);
		
		return new MacdAnalysisConfig(
				shouldMutate(mutatorConfig.mutationChance) ? randomConfig.fastPeriod : config.fastPeriod,
				shouldMutate(mutatorConfig.mutationChance) ? randomConfig.slowPeriod : config.slowPeriod,
				shouldMutate(mutatorConfig.mutationChance) ? randomConfig.signalPeriod : config.signalPeriod
		);
	}
	
	@Override
	public MacdBotConfig validateConfig(final MacdBotConfig config) {
		/*
		 *  Ensures some basic common sense. The genetic algorithm loves to get stuck on an otherwise insane config that just happens to fit the data.
		 */
		
		final MacdAnalysisConfig validBuyAnalysisConfig = validateAnalysisConfig(config.buyAnalysisConfig);
		final MacdAnalysisConfig validSellAnalysisConfig = validateAnalysisConfig(config.sellAnalysisConfig);
		final MacdAnalysisConfig validVolumeAnalysisConfig = validateAnalysisConfig(config.volumeAnalysisConfig);
			
		final MacdBotConfig validConfig = new MacdBotConfig(config.timestep, validBuyAnalysisConfig, validSellAnalysisConfig, validVolumeAnalysisConfig, config.traderConfig);
		
		return validConfig;
	}
	
	public MacdAnalysisConfig validateAnalysisConfig(final MacdAnalysisConfig config) {
		return new MacdAnalysisConfig(
				config.fastPeriod  > config.slowPeriod ?
						SignalMath.getRandomLong(1 * Time.MINUTES, config.slowPeriod) :
						config.fastPeriod,
				config.slowPeriod,
				config.signalPeriod
		);
	}
	
	protected boolean which() {
		return shouldMutate(0.5d);
	}
	
	protected boolean shouldMutate(double chance) {
		return Math.random() < chance;
	}
}
