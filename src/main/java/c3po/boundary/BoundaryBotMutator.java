package c3po.boundary;

import c3po.Training.*;
import c3po.macd.MacdAnalysisConfig;
import c3po.utils.SignalMath;
import c3po.utils.Time;

public class BoundaryBotMutator implements IBotConfigMutator<BoundaryBotConfig> {
	private final BoundaryBotMutatorConfig mutatorConfig;
	private final long timestep;
	
	public BoundaryBotMutator(BoundaryBotMutatorConfig config) {
		this.mutatorConfig = config;
		this.timestep = 1 * Time.MINUTES;
	}

	@Override
	public BoundaryBotConfig createRandomConfig() {

		final MacdAnalysisConfig buyAnalysisConfig = new MacdAnalysisConfig(
				SignalMath.getRandomLong(mutatorConfig.minAnalysisPeriod, mutatorConfig.maxAnalysisPeriod),
				SignalMath.getRandomLong(mutatorConfig.minAnalysisPeriod, mutatorConfig.maxAnalysisPeriod),
				SignalMath.getRandomLong(mutatorConfig.minAnalysisPeriod, mutatorConfig.maxAnalysisPeriod)
		);
		
		final BoundaryTraderConfig traderConfig = new BoundaryTraderConfig(
				SignalMath.getRandomDouble(mutatorConfig.minBuyDiffThreshold, mutatorConfig.maxBuyDiffThreshold),
				SignalMath.getRandomDouble(mutatorConfig.minLossCuttingPercentage, mutatorConfig.maxLossCuttingPercentage)
		);
		
		final BoundaryBotConfig config = new BoundaryBotConfig(timestep, buyAnalysisConfig, traderConfig);
		
		return config;
	}
	
	@Override
	public BoundaryBotConfig crossBreedConfig(final BoundaryBotConfig parentA, final BoundaryBotConfig parentB) {
		
		// Each property is randomly selected from either parent
		
		final MacdAnalysisConfig buyAnalysisConfig = new MacdAnalysisConfig(
				which() ? parentA.buyAnalysisConfig.fastPeriod : parentB.buyAnalysisConfig.fastPeriod,
				which() ? parentA.buyAnalysisConfig.slowPeriod : parentB.buyAnalysisConfig.slowPeriod,
				which() ? parentA.buyAnalysisConfig.signalPeriod : parentB.buyAnalysisConfig.signalPeriod
		);

		final BoundaryTraderConfig traderConfig = new BoundaryTraderConfig(
				which() ? parentA.traderConfig.minBuyDiffThreshold : parentB.traderConfig.minBuyDiffThreshold,
				which() ? parentA.traderConfig.lossCutThreshold : parentB.traderConfig.lossCutThreshold
													
		);
		
		final BoundaryBotConfig childConfig = new BoundaryBotConfig(
				which() ? parentA.timestep : parentB.timestep,
				buyAnalysisConfig,
				traderConfig);
		
		return childConfig;
	}
	
	@Override
	public BoundaryBotConfig mutateConfig(final BoundaryBotConfig config) {
		// Generate a fully random config
		final BoundaryBotConfig randomConfig = createRandomConfig();
		
		// Each property has a separately evaluated chance of changing to the above generated value
		
		final MacdAnalysisConfig buyAnalysisConfig = new MacdAnalysisConfig(
				shouldMutate(mutatorConfig.mutationChance) ? randomConfig.buyAnalysisConfig.fastPeriod : config.buyAnalysisConfig.fastPeriod,
				shouldMutate(mutatorConfig.mutationChance) ? randomConfig.buyAnalysisConfig.slowPeriod : config.buyAnalysisConfig.slowPeriod,
				shouldMutate(mutatorConfig.mutationChance) ? randomConfig.buyAnalysisConfig.signalPeriod : config.buyAnalysisConfig.signalPeriod
		);
			
		final BoundaryTraderConfig traderConfig = new BoundaryTraderConfig(
				shouldMutate(mutatorConfig.mutationChance) ? randomConfig.traderConfig.minBuyDiffThreshold : config.traderConfig.minBuyDiffThreshold,
				shouldMutate(mutatorConfig.mutationChance) ? randomConfig.traderConfig.lossCutThreshold : config.traderConfig.lossCutThreshold
		);
		
		final BoundaryBotConfig mutatedConfig = new BoundaryBotConfig(config.timestep, buyAnalysisConfig, traderConfig);
		
		return mutatedConfig;
	}
	
	@Override
	public BoundaryBotConfig validateConfig(final BoundaryBotConfig config) {
		/*
		 *  Ensures some basic common sense. The genetic algorithm loves to get stuck on an otherwise insane config that just happens to fit the data.
		 */
		
		final MacdAnalysisConfig validBuyAnalysisConfig = new MacdAnalysisConfig(
				config.buyAnalysisConfig.fastPeriod  > config.buyAnalysisConfig.slowPeriod ?
						SignalMath.getRandomLong(1 * Time.MINUTES, config.buyAnalysisConfig.slowPeriod) :
						config.buyAnalysisConfig.fastPeriod,
				config.buyAnalysisConfig.slowPeriod,
				config.buyAnalysisConfig.signalPeriod
		);
			
		final BoundaryBotConfig validConfig = new BoundaryBotConfig(config.timestep, validBuyAnalysisConfig, config.traderConfig);
		
		return validConfig;
	}
	
	protected boolean which() {
		return shouldMutate(0.5d);
	}
	
	protected boolean shouldMutate(double chance) {
		return Math.random() < chance;
	}
}
