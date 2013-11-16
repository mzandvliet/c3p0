package c3po.macd;

public class MacdTraderConfig {
	public final long startDelay; // Used to avoid trading while macdBuffers are still empty and yield unstable signals
	public final double minDiffVelocity;
	public final double usdToBtcTradeAmount;
	public final double btcToUsdTradeAmount;
	
	public MacdTraderConfig(long startDelay, double minDiffVelocity,
			double usdToBtcTradeAmount, double btcToUsdTradeAmount) {
		super();
		this.startDelay = startDelay;
		this.minDiffVelocity = minDiffVelocity;
		this.usdToBtcTradeAmount = usdToBtcTradeAmount;
		this.btcToUsdTradeAmount = btcToUsdTradeAmount;
	}

	public String toString() {
		return String.format("[MacdBotConfig - startDelay: %s, minDiffVelocity: %s, usdToBtcTradeAmount: %s, btcToUsdTradeAmount: %s]", 
				startDelay, 
				(double) Math.round(minDiffVelocity * 10000) / 10000,  
				(double) Math.round(usdToBtcTradeAmount * 10000) / 10000, 
				(double) Math.round(btcToUsdTradeAmount * 10000) / 10000);
	}
}