package c3po.macd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import c3po.ISignal;
import c3po.ITickable;
import c3po.ITradeFloor;
import c3po.Sample;

public class MacdTraderNode implements ITickable {
	private static final Logger LOGGER = LoggerFactory.getLogger(MacdTraderNode.class);
	private final ISignal macdDiff;
	private final ITradeFloor tradeFloor;
	private final MacdTraderConfig config;
	
	private long lastTick;
	private Sample lastDiff;
	
	private long lastBuy = 0l; // Last time we bought in milliseconds epoch
	private long lastSell = 0l; // Last time we sold in milliseconds epoch
	
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
			// We dont want to trade too often, check if we arent in the backoff period
			boolean buyInBackOffTimer = (lastBuy > tick - config.buyBackoffTimer);
			boolean sellInBackOffTimer = (lastSell > tick - config.sellBackoffTimer);
			if (!buyInBackOffTimer && currentDiff.value > config.minBuyDiffThreshold && tradeFloor.getWalledUsd() > minDollars) {
				// Trade half of current usdWallet
				double dollars = tradeFloor.getWalledUsd() * config.usdToBtcTradeAmount;
				double volume = tradeFloor.toBtc(dollars);
				double btcBought = tradeFloor.buy(tick, volume);
				lastBuy = tick;
				
				LOGGER.info(String.format("Bought %s BTC for %s USD because difference %s > %s", btcBought, dollars, currentDiff.value, config.minBuyDiffThreshold));
			}
			else if (!sellInBackOffTimer && currentDiff.value < config.minSellDiffThreshold && tradeFloor.getWalletBtc() > tradeFloor.toBtc(minDollars)) {
				// Trade all of current btcWallet
				double btcToSell = tradeFloor.getWalletBtc() * config.btcToUsdTradeAmount;
				double soldForUSD = tradeFloor.sell(tick, btcToSell);
				lastSell = tick;
				
				LOGGER.info(String.format("Sold %s BTC for %s USD because difference %s < %s", btcToSell, soldForUSD, currentDiff.value, config.minSellDiffThreshold));
				
			}
		}
		else {
			numSkippedTicks++;
		}
		
		lastDiff = currentDiff;
	}
}
