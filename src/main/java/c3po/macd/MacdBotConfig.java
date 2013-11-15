package c3po.macd;

public class MacdBotConfig  {
	private final long startDelay; // Used to avoid trading while macdBuffers are still empty and yield unstable signals
	private final double minDiffVelocity;
	private final double usdToBtcTradeAmount;
	private final double btcToUsdTradeAmount;
	
	public MacdBotConfig(long startDelay, double minDiffVelocity, double usdToBtcTradeAmount, double btcToUsdTradeAmount) {
		this.startDelay = startDelay;
		this.minDiffVelocity = minDiffVelocity;
		this.usdToBtcTradeAmount = usdToBtcTradeAmount;
		this.btcToUsdTradeAmount = btcToUsdTradeAmount;
	}

	public long getStartDelay() {
		return startDelay;
	}

	public double getMinDiffVelocity() {
		return minDiffVelocity;
	}

	public double getUsdToBtcTradeAmount() {
		return usdToBtcTradeAmount;
	}

	public double getBtcToUsdTradeAmount() {
		return btcToUsdTradeAmount;
	}
	
	public String toString() {
		return String.format("[MacdBotConfig - startDelay: %s, minDiffVelocity: %s, usdToBtcTradeAmount: %s, btcToUsdTradeAmount: %s]", 
				startDelay, 
				(double) Math.round(minDiffVelocity * 10000) / 10000,  
				(double) Math.round(usdToBtcTradeAmount * 10000) / 10000, 
				(double) Math.round(btcToUsdTradeAmount * 10000) / 10000);
	}
}
