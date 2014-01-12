package c3po.node;

/**
 * Config for the LossStopNode
 */
public class LossStopNodeConfig {
	/**
	 * This percentage is the amount that will not cause
	 * the tradeAdvice to rise. For instance: 3.0 means,
	 * the current price needs to drop more then 3% under
	 * the highest price for the trade advice to become negative.
	 */
	private final double ignoreLossPercentage;
	
	private final double maxLossPercentage;

	public LossStopNodeConfig(double ignoreLossPercentage, double maxLossPercentage) {
		this.ignoreLossPercentage = ignoreLossPercentage;
		this.maxLossPercentage = maxLossPercentage;
	}

	public double getIgnoreLossPercentage() {
		return ignoreLossPercentage;
	}

	public double getMaxLossPercentage() {
		return maxLossPercentage;
	}

	@Override
	public String toString() {
		return "LossStopNodeConfig [ignoreLossPercentage="
				+ ignoreLossPercentage + ", maxLossPercentage="
				+ maxLossPercentage + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(ignoreLossPercentage);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(maxLossPercentage);
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
		LossStopNodeConfig other = (LossStopNodeConfig) obj;
		if (Double.doubleToLongBits(ignoreLossPercentage) != Double
				.doubleToLongBits(other.ignoreLossPercentage))
			return false;
		if (Double.doubleToLongBits(maxLossPercentage) != Double
				.doubleToLongBits(other.maxLossPercentage))
			return false;
		return true;
	}
}