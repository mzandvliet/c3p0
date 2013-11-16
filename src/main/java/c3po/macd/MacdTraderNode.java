package c3po.macd;

import c3po.ISignal;
import c3po.ITickable;
import c3po.ITradeFloor;
import c3po.Sample;

/* Todo:
 * - Accept MACD signals from signal tree
 * 		- Or just create an internal macd tree and feed it a ticker signal
 * - Interpret according to configuration
 * - Generate buy/sell orders through an ITradeFloor interface or something
 */

public class MacdTraderNode implements ITickable {
	private final ISignal macdDiff;
	private final ITradeFloor tradeFloor;
	private final MacdTraderConfig config;
	
	private long lastTick;
	private Sample lastDiff;

	public MacdTraderNode(ISignal macdDiff, ITradeFloor tradeFloor, MacdTraderConfig config) {
		this.macdDiff = macdDiff;
		this.tradeFloor = tradeFloor;
		this.config = config;
	}	

	public MacdTraderConfig getConfig() {
		return config;
	}
	
	@Override
	public void tick(long tick) {
		if (tick > lastTick) {
			decide(tick);
		}
		lastTick = tick;
	}
	
	/*
	 *  Do trades purely based on zero-crossings in difference signal
	 */
	public void decide(long tick) {
		Sample currentDiff = macdDiff.getSample(tick);
		
		if (tick > config.startDelay) {
			double velocity = lastDiff.value - currentDiff.value;
			double signLast = Math.signum(lastDiff.value);
			double signCurrent = Math.signum(currentDiff.value);
			
			boolean isCrossing = Math.abs(velocity) > config.minDiffVelocity && signLast != signCurrent;
			
			if (!isCrossing)  // We only trade if there is a zero crossing
				return;
			
			if (velocity > 0f) {
				// Trade half of current usdWallet
				double volume = tradeFloor.toBtc(tradeFloor.getWalledUsd() * config.usdToBtcTradeAmount);
				tradeFloor.buy(volume);
			}
			else {
				// Trade all of current btcWallet
				double volume = tradeFloor.toBtc(tradeFloor.getWalletBtc() * config.btcToUsdTradeAmount);
				tradeFloor.sell(volume);
			}
		}
		
		lastDiff = currentDiff;
	}
}
