package c3po.composite;

import c3po.IBotConfig;

public class CompositeBotConfig implements IBotConfig {
	public final long timestep;
	public final double minBuyDiffThreshold;
	public final double minSellDiffThreshold;

	public CompositeBotConfig(
			long timestep,
			double minBuyDiffThreshold,
			double minSellDiffThreshold) {
		this.timestep = timestep;
		this.minBuyDiffThreshold = minBuyDiffThreshold;
		this.minSellDiffThreshold = minSellDiffThreshold;
	}

	public String toString() {
		return String.format("[CompositeConfig - timestep: %d, minBuyThreshold: %,.4f, minSellThreshold: %,.4f]", 
				timestep,
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
		result = prime * result + (int) (timestep ^ (timestep >>> 32));
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
		CompositeBotConfig other = (CompositeBotConfig) obj;
		if (Double.doubleToLongBits(minBuyDiffThreshold) != Double
				.doubleToLongBits(other.minBuyDiffThreshold))
			return false;
		if (Double.doubleToLongBits(minSellDiffThreshold) != Double
				.doubleToLongBits(other.minSellDiffThreshold))
			return false;
		if (timestep != other.timestep)
			return false;
		return true;
	}

	@Override
	public String toEscapedJSON() {
		// TODO Auto-generated method stub
		return null;
	}

	
}