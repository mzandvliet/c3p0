package c3po.macd;

import c3po.Time;

public class MacdTraderConfig {
	public final double minBuyDiffThreshold;
	public final double minSellDiffThreshold;
	
	public MacdTraderConfig(
			double minBuyThreshold,
			double minSellThreshold) {
		super();
		this.minBuyDiffThreshold = minBuyThreshold;
		this.minSellDiffThreshold = minSellThreshold;
	}

	public String toString() {
		return String.format("[TraderConfig - minBuyThreshold: %,.4f, minSellThreshold: %,.4f]", 
				minBuyDiffThreshold,
				minSellDiffThreshold);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
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
		if (Double.doubleToLongBits(minBuyDiffThreshold) != Double
				.doubleToLongBits(other.minBuyDiffThreshold))
			return false;
		if (Double.doubleToLongBits(minSellDiffThreshold) != Double
				.doubleToLongBits(other.minSellDiffThreshold))
			return false;
		return true;
	}
}