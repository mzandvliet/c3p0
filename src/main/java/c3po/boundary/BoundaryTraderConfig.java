package c3po.boundary;

public class BoundaryTraderConfig {
	public final double minBuyDiffThreshold;
	public final double lossCutThreshold;

	

	public BoundaryTraderConfig(double minBuyDiffThreshold,
			double lossCutThreshold) {
		super();
		this.minBuyDiffThreshold = minBuyDiffThreshold;
		this.lossCutThreshold = lossCutThreshold;
	}

	public String toString() {
		return String.format("[TraderConfig - minBuyThreshold: %,.4f, lossCutThreshold at %,.2f]", 
				minBuyDiffThreshold,
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
		BoundaryTraderConfig other = (BoundaryTraderConfig) obj;
		if (Double.doubleToLongBits(lossCutThreshold) != Double
				.doubleToLongBits(other.lossCutThreshold))
			return false;
		if (Double.doubleToLongBits(minBuyDiffThreshold) != Double
				.doubleToLongBits(other.minBuyDiffThreshold))
			return false;
		return true;
	}
}