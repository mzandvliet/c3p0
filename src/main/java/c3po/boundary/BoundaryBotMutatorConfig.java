package c3po.boundary;

import c3po.Training.IBotMutationConfig;

public class BoundaryBotMutatorConfig implements IBotMutationConfig<BoundaryBotConfig>{
	public final double mutationChance;
	public final long minAnalysisPeriod;
	public final long maxAnalysisPeriod;
	public final double minBuyDiffThreshold;
	public final double maxBuyDiffThreshold;
	public final double minLossCuttingPercentage;
	public final double maxLossCuttingPercentage;

	public BoundaryBotMutatorConfig(double mutationChance, long minAnalysisPeriod,
			long maxAnalysisPeriod, double minBuyDiffThreshold,
			double maxBuyDiffThreshold,
			double minLossCuttingPercentage,
			double maxLossCuttingPercentage) {
		this.mutationChance = mutationChance;
		this.minAnalysisPeriod = minAnalysisPeriod;
		this.maxAnalysisPeriod = maxAnalysisPeriod;
		this.minBuyDiffThreshold = minBuyDiffThreshold;
		this.maxBuyDiffThreshold = maxBuyDiffThreshold;
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
		temp = Double.doubleToLongBits(maxLossCuttingPercentage);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result
				+ (int) (minAnalysisPeriod ^ (minAnalysisPeriod >>> 32));
		temp = Double.doubleToLongBits(minBuyDiffThreshold);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(minLossCuttingPercentage);
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
		BoundaryBotMutatorConfig other = (BoundaryBotMutatorConfig) obj;
		if (maxAnalysisPeriod != other.maxAnalysisPeriod)
			return false;
		if (Double.doubleToLongBits(maxBuyDiffThreshold) != Double
				.doubleToLongBits(other.maxBuyDiffThreshold))
			return false;
		if (Double.doubleToLongBits(maxLossCuttingPercentage) != Double
				.doubleToLongBits(other.maxLossCuttingPercentage))
			return false;
		if (minAnalysisPeriod != other.minAnalysisPeriod)
			return false;
		if (Double.doubleToLongBits(minBuyDiffThreshold) != Double
				.doubleToLongBits(other.minBuyDiffThreshold))
			return false;
		if (Double.doubleToLongBits(minLossCuttingPercentage) != Double
				.doubleToLongBits(other.minLossCuttingPercentage))
			return false;
		if (Double.doubleToLongBits(mutationChance) != Double
				.doubleToLongBits(other.mutationChance))
			return false;
		return true;
	}
}
