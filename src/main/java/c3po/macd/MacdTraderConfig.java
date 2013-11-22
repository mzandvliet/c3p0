package c3po.macd;

import c3po.Time;

public class MacdTraderConfig {
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
			double minBuyThreshold,
			double minSellThreshold,
			double usdToBtcTradeAmount,
			double btcToUsdTradeAmount,
			long sellBackoffTimer,
			long buyBackoffTimer) {
		super();
		this.minBuyDiffThreshold = minBuyThreshold;
		this.minSellDiffThreshold = minSellThreshold;
		this.usdToBtcTradeAmount = usdToBtcTradeAmount;
		this.btcToUsdTradeAmount = btcToUsdTradeAmount;
		this.sellBackoffTimer = sellBackoffTimer;
		this.buyBackoffTimer = buyBackoffTimer;
		
	}

	public String toString() {
		return String.format("[TraderConfig - minBuyThreshold: %,.4f, minSellThreshold: %,.4f, usdToBtcTradeAmount: %,.4f, btcToUsdTradeAmount: %,.4f, sellBackoffTimer: %d min, buyBackoffTimer: %d min]", 
				minBuyDiffThreshold,
				minSellDiffThreshold,
				usdToBtcTradeAmount, 
				btcToUsdTradeAmount,
				(long)(sellBackoffTimer / Time.MINUTES),
				(long)(buyBackoffTimer / Time.MINUTES));
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
		if (Double.doubleToLongBits(usdToBtcTradeAmount) != Double
				.doubleToLongBits(other.usdToBtcTradeAmount))
			return false;
		return true;
	}
}