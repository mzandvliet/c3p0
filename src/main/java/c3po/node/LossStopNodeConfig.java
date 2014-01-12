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
}