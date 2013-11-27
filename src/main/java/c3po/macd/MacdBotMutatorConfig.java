package c3po.macd;

import c3po.IBotMutationConfig;

public class MacdBotMutatorConfig implements IBotMutationConfig<MacdBotConfig>{
	public final double mutationChance;
	public final long minAnalysisPeriod;
	public final long maxAnalysisPeriod;
	public final double minBuyDiffThreshold;
	public final double maxBuyDiffThreshold;
	public final double minSellDiffThreshold;
	public final double maxSellDiffThreshold;
	
	public MacdBotMutatorConfig(
			double mutationChance,
			long minAnalysisPeriod,
			long maxAnalysisPeriod,
			double minBuyDiffThreshold,
			double maxBuyDiffThreshold,
			double minSellDiffThreshold,
			double maxSellDiffThreshold) {
		
		super();
		
		this.mutationChance = mutationChance;
		this.minAnalysisPeriod = minAnalysisPeriod;
		this.maxAnalysisPeriod = maxAnalysisPeriod;
		this.minBuyDiffThreshold = minBuyDiffThreshold;
		this.maxBuyDiffThreshold = maxBuyDiffThreshold;
		this.minSellDiffThreshold = minSellDiffThreshold;
		this.maxSellDiffThreshold = maxSellDiffThreshold;
	}
}
