package c3po.macd;

import c3po.Training.IBotMutationConfig;

public class MacdBotMutatorConfig implements IBotMutationConfig<MacdBotConfig>{
	public final double mutationChance;
	public final long minAnalysisPeriod;
	public final long maxAnalysisPeriod;
	public final double minBuyDiffThreshold;
	public final double maxBuyDiffThreshold;
	public final double minSellDiffThreshold;
	public final double maxSellDiffThreshold;
	public final double minSellPercentage;
	public final double maxSellPercentage;
	public final double minBuyPercentage;
	public final double maxBuyPercentage;
	public final long minSellBackoffTimer;
	public final long maxSellBackoffTimer;
	public final long minBuyBackoffTimer;
	public final long maxBuyBackoffTimer;
	public final double minLossCuttingPercentage;
	public final double maxLossCuttingPercentage;	
	
	public MacdBotMutatorConfig(double mutationChance, 
			long minAnalysisPeriod, long maxAnalysisPeriod, 
			double minBuyDiffThreshold, double maxBuyDiffThreshold, 
			double minSellDiffThreshold, double maxSellDiffThreshold,  
			double minBuyPercentage, double maxBuyPercentage,
			double minSellPercentage, double maxSellPercentage,
			long minBuyBackoffTimer, long maxBuyBackoffTimer, 
			long minSellBackoffTimer, long maxSellBackoffTimer,
			double minLossCuttingPercentage, double maxLossCuttingPercentage) {
		this.mutationChance = mutationChance;
		this.minAnalysisPeriod = minAnalysisPeriod;
		this.maxAnalysisPeriod = maxAnalysisPeriod;
		this.minBuyDiffThreshold = minBuyDiffThreshold;
		this.maxBuyDiffThreshold = maxBuyDiffThreshold;
		this.minSellDiffThreshold = minSellDiffThreshold;
		this.maxSellDiffThreshold = maxSellDiffThreshold;
		this.minSellPercentage = minSellPercentage;
		this.maxSellPercentage = maxSellPercentage;
		this.minBuyPercentage = minBuyPercentage;
		this.maxBuyPercentage = maxBuyPercentage;
		this.minSellBackoffTimer = minSellBackoffTimer;
		this.maxSellBackoffTimer = maxSellBackoffTimer;
		this.minBuyBackoffTimer = minBuyBackoffTimer;
		this.maxBuyBackoffTimer = maxBuyBackoffTimer;
		this.minLossCuttingPercentage = minLossCuttingPercentage;
		this.maxLossCuttingPercentage = maxLossCuttingPercentage;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ (int) (maxAnalysisPeriod ^ (maxAnalysisPeriod >>> 32));
		long temp;
		temp = Double.doubleToLongBits(maxBuyDiffThreshold);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(maxSellDiffThreshold);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result
				+ (int) (minAnalysisPeriod ^ (minAnalysisPeriod >>> 32));
		temp = Double.doubleToLongBits(minBuyDiffThreshold);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(minSellDiffThreshold);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(mutationChance);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MacdBotMutatorConfig other = (MacdBotMutatorConfig) obj;
		if (maxAnalysisPeriod != other.maxAnalysisPeriod)
			return false;
		if (Double.doubleToLongBits(maxBuyDiffThreshold) != Double
				.doubleToLongBits(other.maxBuyDiffThreshold))
			return false;
		if (Double.doubleToLongBits(maxSellDiffThreshold) != Double
				.doubleToLongBits(other.maxSellDiffThreshold))
			return false;
		if (minAnalysisPeriod != other.minAnalysisPeriod)
			return false;
		if (Double.doubleToLongBits(minBuyDiffThreshold) != Double
				.doubleToLongBits(other.minBuyDiffThreshold))
			return false;
		if (Double.doubleToLongBits(minSellDiffThreshold) != Double
				.doubleToLongBits(other.minSellDiffThreshold))
			return false;
		if (Double.doubleToLongBits(mutationChance) != Double
				.doubleToLongBits(other.mutationChance))
			return false;
		return true;
	}
	
	
}
