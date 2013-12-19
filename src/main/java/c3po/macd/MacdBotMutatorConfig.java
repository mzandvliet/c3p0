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
	public final double minBuyVolumeThreshold;
	public final double maxBuyVolumeThreshold;
	public final long minSellPricePeriod;
	public final long maxSellPricePeriod;
	public final double minLossCuttingPercentage;
	public final double maxLossCuttingPercentage;
	public final double minSellThresholdRelaxationFactor;
	public final double maxSellThresholdRelaxationFactor;

	public MacdBotMutatorConfig(
			double mutationChance, long minAnalysisPeriod,
			long maxAnalysisPeriod, double minBuyDiffThreshold,
			double maxBuyDiffThreshold, double minSellDiffThreshold,
			double maxSellDiffThreshold, double minLossCuttingPercentage,
			double minBuyVolumeThreshold, double maxBuyVolumeThreshold,
			long minSellPricePeriod, long maxSellPricePeriod,
			double maxLossCuttingPercentage,
			double minSellThresholdRelaxationFactor,
			double maxSellThresholdRelaxationFactor) {
		super();
		this.mutationChance = mutationChance;
		this.minAnalysisPeriod = minAnalysisPeriod;
		this.maxAnalysisPeriod = maxAnalysisPeriod;
		this.minBuyDiffThreshold = minBuyDiffThreshold;
		this.maxBuyDiffThreshold = maxBuyDiffThreshold;
		this.minSellDiffThreshold = minSellDiffThreshold;
		this.maxSellDiffThreshold = maxSellDiffThreshold;
		this.minBuyVolumeThreshold = minBuyVolumeThreshold;
		this.maxBuyVolumeThreshold = maxBuyVolumeThreshold;
		this.minSellPricePeriod = minSellPricePeriod;
		this.maxSellPricePeriod = maxSellPricePeriod;
		this.minLossCuttingPercentage = minLossCuttingPercentage;
		this.maxLossCuttingPercentage = maxLossCuttingPercentage;
		this.minSellThresholdRelaxationFactor = minSellThresholdRelaxationFactor;
		this.maxSellThresholdRelaxationFactor = maxSellThresholdRelaxationFactor;
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
		temp = Double.doubleToLongBits(maxBuyVolumeThreshold);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(maxLossCuttingPercentage);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(maxSellDiffThreshold);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result
				+ (int) (maxSellPricePeriod ^ (maxSellPricePeriod >>> 32));
		temp = Double.doubleToLongBits(maxSellThresholdRelaxationFactor);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result
				+ (int) (minAnalysisPeriod ^ (minAnalysisPeriod >>> 32));
		temp = Double.doubleToLongBits(minBuyDiffThreshold);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(minBuyVolumeThreshold);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(minLossCuttingPercentage);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(minSellDiffThreshold);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result
				+ (int) (minSellPricePeriod ^ (minSellPricePeriod >>> 32));
		temp = Double.doubleToLongBits(minSellThresholdRelaxationFactor);
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
		if (Double.doubleToLongBits(maxBuyVolumeThreshold) != Double
				.doubleToLongBits(other.maxBuyVolumeThreshold))
			return false;
		if (Double.doubleToLongBits(maxLossCuttingPercentage) != Double
				.doubleToLongBits(other.maxLossCuttingPercentage))
			return false;
		if (Double.doubleToLongBits(maxSellDiffThreshold) != Double
				.doubleToLongBits(other.maxSellDiffThreshold))
			return false;
		if (maxSellPricePeriod != other.maxSellPricePeriod)
			return false;
		if (Double.doubleToLongBits(maxSellThresholdRelaxationFactor) != Double
				.doubleToLongBits(other.maxSellThresholdRelaxationFactor))
			return false;
		if (minAnalysisPeriod != other.minAnalysisPeriod)
			return false;
		if (Double.doubleToLongBits(minBuyDiffThreshold) != Double
				.doubleToLongBits(other.minBuyDiffThreshold))
			return false;
		if (Double.doubleToLongBits(minBuyVolumeThreshold) != Double
				.doubleToLongBits(other.minBuyVolumeThreshold))
			return false;
		if (Double.doubleToLongBits(minLossCuttingPercentage) != Double
				.doubleToLongBits(other.minLossCuttingPercentage))
			return false;
		if (Double.doubleToLongBits(minSellDiffThreshold) != Double
				.doubleToLongBits(other.minSellDiffThreshold))
			return false;
		if (minSellPricePeriod != other.minSellPricePeriod)
			return false;
		if (Double.doubleToLongBits(minSellThresholdRelaxationFactor) != Double
				.doubleToLongBits(other.minSellThresholdRelaxationFactor))
			return false;
		if (Double.doubleToLongBits(mutationChance) != Double
				.doubleToLongBits(other.mutationChance))
			return false;
		return true;
	}
}
