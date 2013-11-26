package c3po.macd;

import c3po.*;

import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.jfree.data.time.MovingAverage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import c3po.TradeAction.TradeActionType;
import c3po.bitstamp.BitstampSimulationTradeFloor;
import c3po.ExpMovingAverageNode;
import c3po.IClock;
import c3po.ISignal;
import c3po.ITradeFloor;
import c3po.ITradeListener;
import c3po.IWallet;
import c3po.Sample;
import c3po.TradeAction;
import c3po.bitstamp.BitstampTickerCsvSource;

/**
 * Very simple Bot implementation that takes a running average 
 * of a period of time. If that surpasses a threshold it buys or sells.
 */

public class SimpleTrendBot extends AbstractTickable implements IBot {
	//================================================================================
    // Static Properties
    //================================================================================
	
	private static final Logger LOGGER = LoggerFactory.getLogger(SimpleTrendBot.class);
	
	//private final static String jsonUrl = "http://www.bitstamp.net/api/ticker/";
	
	private final static String csvPath = "resources/bitstamp_ticker_till_20131122_crashed.csv";
	private final static long simulationStartTime = 1384079023000l;
	private final static long simulationEndTime = 1385192429000l; 
	private final static long interpolationTime = 2 * Time.MINUTES; // Delay data by two minutes for interpolation
	
	private final static long timestep = 1 * Time.MINUTES;
	
	private final static double walletDollarStart = 100.0d;
	private final static double walletBtcStart = 0.0d;
	
	private final static long graphInterval = 10 * Time.MINUTES;

	private static final double minDollars = 1.0;
	
	//================================================================================
    // Main
    //================================================================================
	
	public static void main(String[] args) throws ClassNotFoundException, SQLException {
		// Set up global signal tree
		
		final BitstampTickerCsvSource tickerNode = new BitstampTickerCsvSource(timestep, interpolationTime, csvPath);
			
		final IWallet wallet = new Wallet(walletDollarStart, walletBtcStart);
		
		final ITradeFloor tradeFloor =  new BitstampSimulationTradeFloor(
				tickerNode.getOutputLast(),
				tickerNode.getOutputBid(),
				tickerNode.getOutputAsk()
		);
		
		// Create bot config
		MacdTraderConfig traderConfig = new MacdTraderConfig(
				0.05,
				-0.05,
				0.1,
				0.2,
				10 * Time.MINUTES,
				10 * Time.MINUTES
		);

		// Create bot
		SimpleTrendBot bot = new SimpleTrendBot(traderConfig, timestep, 33 * Time.MINUTES, tickerNode.getOutputHigh(), wallet, tradeFloor);
		
		// Create loggers
		
		DebugTradeLogger tradeLogger = new DebugTradeLogger();
		bot.addTradeListener(tradeLogger);
		
		DbTradeLogger dbTradeLogger = new DbTradeLogger(bot, new InetSocketAddress("94.208.87.249", 3309), "c3po", "D7xpJwzGJEWf5qWB");
		dbTradeLogger.open();
		dbTradeLogger.startSession(simulationStartTime, walletDollarStart, walletBtcStart);

		
		// Create the grapher
		
		GraphingNode grapher = new GraphingNode(graphInterval, "MacdBot", 
				tickerNode.getOutputHigh(),
				bot.getAnalysisNode().getOutput(0),
				bot.getAnalysisNode().getOutput(1)
				);
		grapher.pack();
		grapher.setVisible(true);
		bot.addTradeListener(grapher);
		
		// Create a clock
		
		IClock botClock = new SimulationClock(timestep, simulationStartTime, simulationEndTime, interpolationTime);
		botClock.addListener(bot);
		botClock.addListener(grapher);
		
		// Run the program
		
		tickerNode.open();
		botClock.run();
		tickerNode.close();
		
		
		// Log results
		
		LOGGER.debug("Num trades: " + tradeLogger.getActions().size() + ", Profit: " + (tradeFloor.getWalletValueInUsd(wallet) - walletDollarStart));
		tradeLogger.writeLog();
	}
	
	
	//================================================================================
    // Properties
    //================================================================================
	
	private final MacdTraderConfig config;
	private final IWallet wallet;
	private final ITradeFloor tradeFloor;
	
	private final List<ITradeListener> listeners;
	
    // Debug references
	
	private final ExpMovingAverageNode movingAvgNode;

	private long startMoment;
	private long window;

	private long lastBuyTime;

	private long lastSellTime;
	
	private Sample prevAvg;
	

	//================================================================================
    // Methods
    //================================================================================
	
	public SimpleTrendBot(MacdTraderConfig config, long timestep, long window, ISignal ticker, IWallet wallet, ITradeFloor tradeFloor) {
		super(timestep);
		this.config = config;
		this.wallet = wallet;
		this.tradeFloor = tradeFloor;
		this.window = window;
		
		this.listeners = new LinkedList<ITradeListener>();
		
		// Define the signal tree		
		this.movingAvgNode = new ExpMovingAverageNode(timestep, window, ticker);
	}

	@Override
	public void onNewTick(long tick) {
		tradeFloor.updateWallet(wallet);		
		movingAvgNode.tick(tick);
		decide(tick);
	}
	
	/*
	 *  Do trades purely based on zero-crossings in difference signal
	 */
	public void decide(long tick) {
		Sample currentAvg = movingAvgNode.getOutput(1).getSample(tick);

		if(this.startMoment == 0)
			this.startMoment = tick;
		
		if (startMoment + window < tick) {
			
			double diffV =  Math.round((currentAvg.value - prevAvg.value) * 1000.0d) / 1000.0d;
			double diffT = (currentAvg.timestamp - prevAvg.timestamp) / 60000.0d;
			double currentDiff = Math.round(diffV /  diffT * 1000.0d) / 1000.0d;
			
			LOGGER.debug(String.format("Diff V: %s, Diff T: %s, Diff: %s", diffV, diffT, currentDiff));
			
			// We don't want to trade too often, check if we are not in the backoff period
			boolean buyBackOff = (lastBuyTime > tick - config.buyBackoffTimer);
			boolean sellBackOff = (lastSellTime > tick - config.sellBackoffTimer);
			
			if (!buyBackOff && currentDiff > config.minBuyDiffThreshold && wallet.getWalletUsd() > minDollars) {
				double dollars = wallet.getWalletUsd() * config.usdToBtcTradeAmount;
				TradeAction buyAction = new TradeAction(TradeActionType.BUY, tick, dollars);
				double btcBought = tradeFloor.buy(wallet, buyAction);
				
				lastBuyTime = tick;
				
				notify(buyAction);
				LOGGER.info(String.format("Bought %s BTC for %s USD because difference %s > %s", btcBought, dollars, currentDiff, config.minBuyDiffThreshold));
			}
			else if (!sellBackOff && currentDiff < config.minSellDiffThreshold && wallet.getWalletBtc() > tradeFloor.toBtc(minDollars)) {
				double btcToSell = wallet.getWalletBtc() * config.btcToUsdTradeAmount;
				TradeAction sellAction = new TradeAction(TradeActionType.SELL, tick, btcToSell);
				double soldForUSD = tradeFloor.sell(wallet, sellAction);
				
				lastSellTime = tick;
				
				notify(sellAction);
				LOGGER.info(String.format("Sold %s BTC for %s USD because difference %s < %s", btcToSell, soldForUSD, currentDiff, config.minSellDiffThreshold));
			}

		}
		else {
			LOGGER.debug("Startup delay, ignoring tick "+ tick );
		}
		
		
		prevAvg = currentAvg;
	}
	

	@Override
	public long getTimestep() {
		return timestep;
	}

	@Override
	public IWallet getWallet() {
		return wallet;
	}

	@Override
	public ITradeFloor getTradeFloor() {
		return tradeFloor;
	}
	
	public ExpMovingAverageNode getAnalysisNode() {
		return movingAvgNode;
	}
	
	
	public MacdTraderConfig getConfig() {
		return config;
	}

	public String toString() {
		return String.format("SimpleTrendNode - Window: %s, %s", window, config);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((config == null) ? 0 : config.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SimpleTrendBot other = (SimpleTrendBot) obj;
		if (config == null) {
			if (other.config != null)
				return false;
		} else if (!config.equals(other.config))
			return false;
		return true;
	}

	private void notify(TradeAction action) {
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
