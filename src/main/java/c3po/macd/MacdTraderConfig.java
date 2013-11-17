package c3po.macd;

public class MacdTraderConfig {
	public final long startDelay; // Used to avoid trading while macdBuffers are still empty and yield unstable signals
	public final double minBuyVelocity;
	public final double minSellVelocity;
	public final double usdToBtcTradeAmount;
	public final double btcToUsdTradeAmount;
	
	public MacdTraderConfig(
			long startDelay,
			double minBuyVelocity,
			double minSellVelocity,
			double usdToBtcTradeAmount,
			double btcToUsdTradeAmount) {
		super();
		this.startDelay = startDelay;
		this.minBuyVelocity = minBuyVelocity;
		this.minSellVelocity = minSellVelocity;
		this.usdToBtcTradeAmount = usdToBtcTradeAmount;
		this.btcToUsdTradeAmount = btcToUsdTradeAmount;
	}

	public String toString() {
		return String.format("[MacdBotConfig - startDelay: %s, minBuyVelocity: %s, minSellVelocity: %s, usdToBtcTradeAmount: %s, btcToUsdTradeAmount: %s]", 
				startDelay, 
				(double) Math.round(minBuyVelocity * 10000) / 10000,
				(double) Math.round(minSellVelocity * 10000) / 10000,
				(double) Math.round(usdToBtcTradeAmount * 10000) / 10000, 
				(double) Math.round(btcToUsdTradeAmount * 10000) / 10000);
	}
}