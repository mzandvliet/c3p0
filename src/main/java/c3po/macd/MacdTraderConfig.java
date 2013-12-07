package c3po.macd;

public class MacdTraderConfig {
	public final double minBuyDiffThreshold;
	public final double minSellDiffThreshold;
	public final double sellPercentage;
	public final double buyPercentage;
	public final long sellBackoffTimer;
	public final long buyBackoffTimer;
	public final double lossCuttingPercentage;
	
	public MacdTraderConfig(
			double minBuyThreshold,
			double minSellThreshold,
			double buyPercentage,
			double sellPercentage,
			long sellBackoffTimer,
			long buyBackoffTimer,
			double lossCuttingPercentage) {
		this.minBuyDiffThreshold = minBuyThreshold;
		this.minSellDiffThreshold = minSellThreshold;
		this.buyPercentage = buyPercentage;
		this.sellPercentage = sellPercentage;
		this.sellBackoffTimer = sellBackoffTimer;
		this.buyBackoffTimer = buyBackoffTimer;
		this.lossCuttingPercentage = lossCuttingPercentage;
	}

	public String toString() {
		return String.format("[TraderConfig - minBuyThreshold: %,.4f, minSellThreshold: %,.4f, buyPercentage: %,.2f max every %d min, sellPercentage: %,.2f max every %d min. Cutting losses at %,.2f]", 
				minBuyDiffThreshold,
				minSellDiffThreshold,
				buyPercentage,
				buyBackoffTimer / 1000 / 60,
				sellPercentage,
				sellBackoffTimer / 1000 / 60,
				lossCuttingPercentage);
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