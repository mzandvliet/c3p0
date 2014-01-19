package c3po.composite;

import java.util.ArrayList;
import java.util.List;

import c3po.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import c3po.structs.TradeIntention;
import c3po.structs.TradeIntention.TradeActionType;
import c3po.structs.TradeResult;
import c3po.wallet.IWallet;
import c3po.ITradeFloor;
import c3po.ITradeListener;

public class CompositeBot extends AbstractTickable implements IBot<CompositeBotConfig>, ITradeActionSource {
	private static final Logger LOGGER = LoggerFactory.getLogger(CompositeBot.class);
	private final int id;
	private final IWallet wallet;
	private final ITradeFloor tradeFloor;
	private final List<WeightedTradeAdviceSignal> tradeSignals;
	private int numSkippedTicks;
	private long startDelay;
	private final List<ITradeListener> listeners;
	private final CompositeBotConfig config;

	//================================================================================
    // Methods
    //================================================================================
	
	public CompositeBot(int id, CompositeBotConfig config, List<WeightedTradeAdviceSignal> tradeSignals, IWallet wallet, ITradeFloor tradeFloor) {
		super(config.timestep);
		this.config = config;
		this.id = id;
		this.wallet = wallet;
		this.tradeFloor = tradeFloor;
		this.tradeSignals = tradeSignals;
		
		// TODO Startdelay is max startdelay of all signals
		this.startDelay = config.timestep;
		
		this.listeners = new ArrayList<ITradeListener>();
	}

	@Override
	public void update(long tick) {
		tradeFloor.updateWallet(wallet);
		tradeFloor.adjustOrders();
		decide(tick);
	}
	
	private final static double minDollars = 1.0d;
	
	public void decide(long tick) {

		
		if (numSkippedTicks > startDelay) {
			double score = 0;
			for(WeightedTradeAdviceSignal advice : tradeSignals) {
				score += advice.getWeightedAdviceSample(tick).value;
			}
			
			boolean hasEnoughUsd = wallet.getUsdAvailable() > minDollars;
			boolean hasEnoughBtc = wallet.getBtcAvailable() > tradeFloor.toBtc(tick, minDollars);	
			boolean buyThresholdReached = score > config.minBuyDiffThreshold;
			boolean sellThresholdReached = score < config.minSellDiffThreshold;
			
			
			if(hasEnoughUsd && buyThresholdReached) {
				tryToOpenPosition(tick);
			}
			
			if(hasEnoughBtc && sellThresholdReached) {
				tryToClosePosition(tick);
			}			
		}
		else {
			numSkippedTicks++;
		}
	}
	
	private void tryToOpenPosition(long tick) {
		double usdToSell = wallet.getUsdAvailable();
		TradeIntention buyAction = new TradeIntention(TradeActionType.BUY, tick, usdToSell);
		TradeResult tradeResult = tradeFloor.buy(tick, wallet, buyAction);

		//double currentAveragePrice = averagePrice.getOutput(0).getSample(tick).value;
		//this.lastBuyPrice = currentAveragePrice;
		//this.lastHighestPositionPrice = currentAveragePrice;
		
		//notify(buyAction);
	}

	private void tryToClosePosition(long tick) {
			
			double btcToSell = wallet.getBtcAvailable(); 
			TradeIntention sellAction = new TradeIntention(TradeActionType.SELL, tick, btcToSell);
			TradeResult tradeResult = tradeFloor.sell(tick, wallet, sellAction);
			
			//this.lastBuyPrice = -1;
			//this.lastHighestPositionPrice = -1;

			//notify(sellAction);
	}
	
	@Override
	public long getTimestep() {
		return config.timestep;
	}

	@Override
	public IWallet getWallet() {
		return wallet;
	}

	@Override
	public ITradeFloor getTradeFloor() {
		return tradeFloor;
	}
	
	@Override
	public int getId() {
		return id;
	}
	
	public CompositeBotConfig getConfig() {
		return config;
	}

	public String toString() {
		return String.format("Bot ID: %s, Config: [%s]", id, tradeSignals);
	}

	private void notify(TradeResult action) {
		for (ITradeListener listener : listeners) {
			listener.onTrade(action);
		}
	}

	@Override
	public void addTradeListener(ITradeListener listener) {
		listeners.add(listener);
	}

	@Override
	public void removeListener(ITradeListener listener) {
		listeners.remove(listener);
	}
	
}
