package c3po;

/* Todo:
 * - Accept MACD signals from signal tree
 * 		- Or just create an internal macd tree and feed it a ticker signal
 * - Interpret according to configuration
 * - Generate buy/sell orders through an ITradeFloor interface or something
 */

public class MacdBot implements ITickable {
	private long startDelay; // Used to avoid trading while macdBuffers are still empty and yield unstable signals
	private ISignal macdDiff;
	private ITradeFloor tradeFloor;
	private double minDiffVelocity;
	private Sample lastDiff;
	
	
	public MacdBot(ISignal macdDiff, ITradeFloor tradeFloor, double minDiffVelocity, long startDelay) {
		this.macdDiff = macdDiff;
		this.tradeFloor = tradeFloor;
		this.minDiffVelocity = minDiffVelocity;
		this.startDelay = startDelay;
	}

	/*
	 *  Do trades purely on zerocrossings in diff, lol
	 */
	@Override
	public void tick(long tick) {
		Sample currentDiff = macdDiff.getSample(tick);
		
		if (tick > startDelay) {
			double velocity = lastDiff.value - currentDiff.value;
			double signLast = Math.signum(lastDiff.value);
			double signCurrent = Math.signum(currentDiff.value);
			
			boolean isCrossing = Math.abs(velocity) > minDiffVelocity && signLast != signCurrent;
			
			if (!isCrossing)  // We only trade if there is a zero crossing
				return;
			
			if (velocity > 0f) {
				// Trade half of current usdWallet
				double volume = tradeFloor.toBtc(tradeFloor.getWalledUsd() * 0.5);
				tradeFloor.buy(volume);
			}
			else {
				// Trade all of current btcWallet
				double volume = tradeFloor.toBtc(tradeFloor.getWalletBtc() * 0.5);
				tradeFloor.sell(volume);
			}
		}
		
		lastDiff = currentDiff;
	}
}
