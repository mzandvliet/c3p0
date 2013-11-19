package c3po.macd;

public class MacdTraderConfig {
	public final long startDelay; // Used to avoid trading while macdBuffers are still empty and yield unstable signals
	public final double minBuyDiffThreshold;
	public final double minSellDiffThreshold;
	public final double usdToBtcTradeAmount;
	public final double btcToUsdTradeAmount;
	
	/**
	 * The amount of milliseconds after a sell order
	 * after which the bot will not do any
	 * other sell orders.
	 */
	public final long sellBackoffTimer;
	
	/**
	 * The amount of milliseconds after a buy order
	 * after which the bot will not do any
	 * other buy orders.
	 */
	public final long buyBackoffTimer;
	
	public MacdTraderConfig(
			long startDelay,
			double minBuyVelocity,
			double minSellVelocity,
			double usdToBtcTradeAmount,
			double btcToUsdTradeAmount,
			long sellBackoffTimer,
			long buyBackoffTimer) {
		super();
		this.startDelay = startDelay;
		this.minBuyDiffThreshold = minBuyVelocity;
		this.minSellDiffThreshold = minSellVelocity;
		this.usdToBtcTradeAmount = usdToBtcTradeAmount;
		this.btcToUsdTradeAmount = btcToUsdTradeAmount;
		this.sellBackoffTimer = sellBackoffTimer;
		this.buyBackoffTimer = buyBackoffTimer;
		
	}

	public String toString() {
		return String.format("[TraderConfig - startDelay: %s, minBuyThreshold: %s, minSellThreshold: %s, usdToBtcTradeAmount: %s, btcToUsdTradeAmount: %s, sellBackoffTimer: %ss, buyBackoffTimer: %ss]", 
				startDelay, 
				(double) Math.round(minBuyDiffThreshold * 10000) / 10000,
				(double) Math.round(minSellDiffThreshold * 10000) / 10000,
				(double) Math.round(usdToBtcTradeAmount * 10000) / 10000, 
				(double) Math.round(btcToUsdTradeAmount * 10000) / 10000,
				sellBackoffTimer / 1000, 
				buyBackoffTimer / 1000);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(btcToUsdTradeAmount);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result
				+ (int) (buyBackoffTimer ^ (buyBackoffTimer >>> 32));
		temp = Double.doubleToLongBits(minBuyDiffThreshold);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(minSellDiffThreshold);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result
				+ (int) (sellBackoffTimer ^ (sellBackoffTimer >>> 32));
		result = prime * result + (int) (startDelay ^ (startDelay >>> 32));
		temp = Double.doubleToLongBits(usdToBtcTradeAmount);
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
		if (Double.doubleToLongBits(btcToUsdTradeAmount) != Double
				.doubleToLongBits(other.btcToUsdTradeAmount))
			return false;
		if (buyBackoffTimer != other.buyBackoffTimer)
			return false;
		if (Double.doubleToLongBits(minBuyDiffThreshold) != Double
				.doubleToLongBits(other.minBuyDiffThreshold))
			return false;
		if (Double.doubleToLongBits(minSellDiffThreshold) != Double
				.doubleToLongBits(other.minSellDiffThreshold))
			return false;
		if (sellBackoffTimer != other.sellBackoffTimer)
			return false;
		if (startDelay != other.startDelay)
			return false;
		if (Double.doubleToLongBits(usdToBtcTradeAmount) != Double
				.doubleToLongBits(other.usdToBtcTradeAmount))
			return false;
		return true;
	}
}