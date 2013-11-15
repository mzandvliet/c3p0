package c3po;

import c3po.macd.MacdBotConfig;

/* Todo:
 * - Accept MACD signals from signal tree
 * 		- Or just create an internal macd tree and feed it a ticker signal
 * - Interpret according to configuration
 * - Generate buy/sell orders through an ITradeFloor interface or something
 */

public class MacdBot implements ITickable {
	private final ISignal macdDiff;
	private final ITradeFloor tradeFloor;
	private final MacdBotConfig config;

	private Sample lastDiff;

	public MacdBot(ISignal macdDiff, ITradeFloor tradeFloor, MacdBotConfig config) {
		this.macdDiff = macdDiff;
		this.tradeFloor = tradeFloor;
		this.config = config;
	}	

	public MacdBotConfig getConfig() {
		return config;
	}

	/*
	 *  Do trades purely on zerocrossings in diff, lol
	 */
	@Override
	public void tick(long tick) {
		Sample currentDiff = macdDiff.getSample(tick);
		
		if (tick > config.getStartDelay()) {
			double velocity = lastDiff.value - currentDiff.value;
			double signLast = Math.signum(lastDiff.value);
			double signCurrent = Math.signum(currentDiff.value);
			
			boolean isCrossing = Math.abs(velocity) > config.getMinDiffVelocity() && signLast != signCurrent;
			
			if (!isCrossing)  // We only trade if there is a zero crossing
				return;
			
			if (velocity > 0f) {
				// Trade half of current usdWallet
				double volume = tradeFloor.toBtc(tradeFloor.getWalledUsd() * config.getUsdToBtcTradeAmount());
				tradeFloor.buy(volume);
			}
			else {
				// Trade all of current btcWallet
				double volume = tradeFloor.toBtc(tradeFloor.getWalletBtc() * config.getBtcToUsdTradeAmount());
				tradeFloor.sell(volume);
			}
		}
		
		lastDiff = currentDiff;
	}
}
