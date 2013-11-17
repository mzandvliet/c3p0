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
	
	private final static double minDollars = 1.0d;
	
	/*
	 *  Do trades purely based on zero-crossings in difference signal
	 */
	public void decide(long tick) {
		Sample currentDiff = macdDiff.getSample(tick);
		
		if (numSkippedTicks > config.startDelay) {
			if (currentDiff.value > config.minBuyVelocity && tradeFloor.getWalledUsd() > minDollars) {
				// Trade half of current usdWallet
				double dollars = tradeFloor.getWalledUsd() * config.usdToBtcTradeAmount;
				double volume = tradeFloor.toBtc(dollars);
				double btcBought = tradeFloor.buy(tick, volume);
				LOGGER.info(String.format("Bought %s BTC for %s USD because velocity %s > %s", btcBought, dollars, currentDiff.value, config.minBuyVelocity));
			}
			else if (currentDiff.value < config.minSellVelocity && tradeFloor.getWalletBtc() > tradeFloor.toBtc(minDollars)) {
				// Trade all of current btcWallet
				double btcToSell = tradeFloor.getWalletBtc() * config.btcToUsdTradeAmount;
				double soldForUSD = tradeFloor.sell(tick, btcToSell);
				LOGGER.info(String.format("Sold %s BTC for %s USD because velocity %s < %s", btcToSell, soldForUSD, currentDiff.value, config.minSellVelocity));
			}
		}
		else {
			numSkippedTicks++;
		}
		
		lastDiff = currentDiff;
	}
}
