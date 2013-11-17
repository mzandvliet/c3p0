package c3po.macd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	private static final Logger LOGGER = LoggerFactory.getLogger(MacdTraderNode.class);
	private final ISignal macdDiff;
	private final ITradeFloor tradeFloor;
	private final MacdTraderConfig config;
	
	private long lastTick;
	private Sample lastDiff;
	
	private long numSkippedTicks;

	public MacdTraderNode(ISignal macdDiff, ITradeFloor tradeFloor, MacdTraderConfig config) {
		this.macdDiff = macdDiff;
		this.tradeFloor = tradeFloor;
		this.config = config;
	}	

	public MacdTraderConfig getConfig() {
		return config;
	}
	
	@Override
	public long getLastTick() {
		return lastTick;
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
		
		if (numSkippedTicks > config.startDelay) {
			double velocity = lastDiff.value - currentDiff.value;
			double signLast = Math.signum(lastDiff.value);
			double signCurrent = Math.signum(currentDiff.value);
			
			boolean isCrossing = Math.abs(velocity) > config.minDiffVelocity && signLast != signCurrent;
			
			if (!isCrossing)  // We only trade if there is a zero crossing
				return;
			
			if (velocity > 0f) {
				// Trade half of current usdWallet
				double dollars = tradeFloor.getWalledUsd() * config.usdToBtcTradeAmount;
				double volume = tradeFloor.toBtc(dollars);
				double btcBought = tradeFloor.buy(volume);
				LOGGER.info(String.format("Bought %s BTC for %s USD because velocity %s > %s", btcBought, dollars, velocity, config.minDiffVelocity));
			}
			else {
				// Trade all of current btcWallet
				double btcToSell = tradeFloor.getWalletBtc() * config.btcToUsdTradeAmount;
				double soldForUSD = tradeFloor.sell(btcToSell);
				LOGGER.info(String.format("Sold %s BTC for %s USD because velocity %s < -%s", btcToSell, soldForUSD, velocity, config.minDiffVelocity));
			}
		}
		else {
			numSkippedTicks++;
		}
		
		lastDiff = currentDiff;
	}
}
