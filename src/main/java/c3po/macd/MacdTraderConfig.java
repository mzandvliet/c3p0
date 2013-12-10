package c3po.macd;

public class MacdTraderConfig {
	public final double minBuyDiffThreshold;
	public final double minSellDiffThreshold;
	public final double lossCutThreshold;
	
	public MacdTraderConfig(
			double minBuyThreshold,
			double minSellThreshold,
			double lossCuttingPercentage) {
		this.minBuyDiffThreshold = minBuyThreshold;
		this.minSellDiffThreshold = minSellThreshold;
		this.lossCutThreshold = lossCuttingPercentage;
	}

	public String toString() {
		return String.format("[TraderConfig - minBuyThreshold: %,.4f, minSellThreshold: %,.4f, lossCutThreshold at %,.2f]", 
				minBuyDiffThreshold,
				minSellDiffThreshold,
				lossCutThreshold);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(lossCutThreshold);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(minBuyDiffThreshold);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(minSellDiffThreshold);
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
		MacdTraderConfig other = (MacdTraderConfig) obj;
		if (Double.doubleToLongBits(lossCutThreshold) != Double
				.doubleToLongBits(other.lossCutThreshold))
			return false;
		if (Double.doubleToLongBits(minBuyDiffThreshold) != Double
				.doubleToLongBits(other.minBuyDiffThreshold))
			return false;
		if (Double.doubleToLongBits(minSellDiffThreshold) != Double
				.doubleToLongBits(other.minSellDiffThreshold))
			return false;
		return true;
	}
}