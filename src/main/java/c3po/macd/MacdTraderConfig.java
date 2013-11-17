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
		return String.format("[MacdBotConfig - startDelay: %s, minBuyThreshold: %s, minSellThreshold: %s, usdToBtcTradeAmount: %s, btcToUsdTradeAmount: %s, sellBackoffTimer: %ss, buyBackoffTimer: %ss]", 
				startDelay, 
				(double) Math.round(minBuyDiffThreshold * 10000) / 10000,
				(double) Math.round(minSellDiffThreshold * 10000) / 10000,
				(double) Math.round(usdToBtcTradeAmount * 10000) / 10000, 
				(double) Math.round(btcToUsdTradeAmount * 10000) / 10000,
				sellBackoffTimer / 1000, 
				buyBackoffTimer / 1000);
	}
}