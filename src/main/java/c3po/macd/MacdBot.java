package c3po.macd;

import c3po.*;

import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import c3po.BitstampTickerCsvSource;
import c3po.BitstampSimulationTradeFloor;
import c3po.IClock;
import c3po.ISignal;
import c3po.ITradeFloor;
import c3po.ITradeListener;
import c3po.IWallet;

/* Todo:
 * 
 * ------ Important -----
 * 
 * - Implement better polling to mitigate server data misses
 * 
 * - MacdAnalysisConfig is still expressed in number-of-ticks, which means it depends on bot's timeStep. Should change to units of time. * 
 * 
 * - Manage time duration of open positions
 * 		- Build risk into macd with volatility node
 * 		- High volatility means bot should close positions faster to mitigate crash risk
 * 
 * ----------------------
 * 
 * 
 * - Develop exit protocol and implementation
 * - Seed newly started bots with data from recorded history, then update with live feed
 * 
 * - Use varying macd configurations to fit current market context
 * 
 * - Network architecture
 * 		- INode should define setInput() to assign signals dynamically
 * 			- Constructor should not take input signals anymore
 * 		- Make Sample generic?
 * 			- So you can have ISignal<TradeAction> & INode<TradeAction>
 * 			- Analyse wallet & trade data streams using network methods too
 * 		- Find abstractions that reduce boilerplate for data transformation nodes
 * 			- Something akin to delegates/lambdas, you know
 * 
 * - Improve TradeFloor interface
 * 		- Currency abstraction
 * 
 */

public class MacdBot extends AbstractTickable implements IBot {
	//================================================================================
    // Static Properties
    //================================================================================
	
	private static final Logger LOGGER = LoggerFactory.getLogger(MacdBot.class);
	
	//private final static String jsonUrl = "http://www.bitstamp.net/api/ticker/";
	
	private final static String csvPath = "resources/bitstamp_ticker_till_20131122_crashed.csv";
	private final static long simulationStartTime = 1384079023000l;
	private final static long simulationEndTime = 1385192429000l; 
	private final static long interpolationTime = 2 * Time.MINUTES; // Delay data by two minutes for interpolation
	
	private final static long timestep = 1 * Time.MINUTES;
	private final static double walletDollarStart = 500.0;
	private final static double walletBtcStart = 3.0;
	
	private final static long graphInterval = 4 * Time.MINUTES;
	
	//================================================================================
    // Main
    //================================================================================
	
	public static void main(String[] args) throws ClassNotFoundException, SQLException {
		// Set up global signal tree
		
		//final ISignalSource tickerNode = new BitstampTickerJsonSource(jsonUrl);
		//final BitstampTickerDbSource tickerNode = new BitstampTickerDbSource(interpolationTime, new InetSocketAddress("94.208.87.249", 3309), "c3po", "D7xpJwzGJEWf5qWB");
		final BitstampTickerCsvSource tickerNode = new BitstampTickerCsvSource(timestep, interpolationTime, csvPath);
			
		final IWallet wallet = new Wallet(walletDollarStart, walletBtcStart);
		
		final ITradeFloor tradeFloor =  new BitstampSimulationTradeFloor(
				tickerNode.getOutputLast(),
				tickerNode.getOutputBid(),
				tickerNode.getOutputAsk()
		);
		
		// Create bot config
		
		// todo: this is still in number-of-ticks, which means it depends on bot's timeStep, should change to units of time
		//  Best bot was: [AnalysisConfig - fast: 158 min, slow: 165 min, signal: 903 min], [TraderConfig - 
		MacdAnalysisConfig analysisConfig = new MacdAnalysisConfig(
				(int) (158 * Time.MINUTES),
				(int) (165 * Time.MINUTES),
				(int) (903 * Time.MINUTES));
		
		//minBuyThreshold: 0.761, minSellThreshold: -0.461, usdToBtcTradeAmount: 0.131, btcToUsdTradeAmount: 0.5972, sellBackoffTimer: 3717s, buyBackoffTimer: 73125s]
		MacdTraderConfig traderConfig = new MacdTraderConfig(0.161, -0.261,  0.131, 0.5972, 3717 * Time.MINUTES, 3717 * Time.MINUTES);
		MacdBotConfig config = new MacdBotConfig(timestep, analysisConfig, traderConfig);
		
		// Create bot
		
		MacdBot bot = new MacdBot(config, tickerNode.getOutputHigh(), wallet, tradeFloor);
		
		// Create loggers
		
		DebugTradeLogger tradeLogger = new DebugTradeLogger();
		bot.addTradeListener(tradeLogger);
//		
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
		
//		dbTradeLogger.close();
		
		// Log results
		
		LOGGER.debug("Num trades: " + tradeLogger.getActions().size() + ", Profit: " + (tradeFloor.getWalletValueInUsd(wallet) - walletDollarStart));
		tradeLogger.writeLog();
	}
	
	
	//================================================================================
    // Properties
    //================================================================================
	
	private final MacdBotConfig config;
	private final IWallet wallet;
	private final ITradeFloor tradeFloor;
	
    // Debug references
	
	private final MacdAnalysisNode analysisNode;
	private final MacdTraderNode traderNode;
	

	//================================================================================
    // Methods
    //================================================================================
	
	public MacdBot(MacdBotConfig config, ISignal ticker, IWallet wallet, ITradeFloor tradeFloor) {
		super(config.timestep);
		this.config = config;
		this.wallet = wallet;
		this.tradeFloor = tradeFloor;
		
		// Define the signal tree		
		
		analysisNode = new MacdAnalysisNode(timestep, ticker, config.analysisConfig);
		long startDelayInTicks = config.analysisConfig.max() / timestep;
		traderNode = new MacdTraderNode(timestep, analysisNode.getOutput(4), wallet, tradeFloor, config.traderConfig, startDelayInTicks);
	}

	@Override
	public void onNewTick(long tick) {
		traderNode.tick(tick);
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
	
	public MacdAnalysisNode getAnalysisNode() {
		return analysisNode;
	}
	
	public MacdTraderNode getTraderNode() {
		return traderNode;
	}
	
	public MacdBotConfig getConfig() {
		return config;
	}

	public String toString() {
		return String.format("%s, %s", analysisNode.getConfig(), traderNode.getConfig());
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
		MacdBot other = (MacdBot) obj;
		if (config == null) {
			if (other.config != null)
				return false;
		} else if (!config.equals(other.config))
			return false;
		return true;
	}

	@Override
	public void addTradeListener(ITradeListener listener) {
		traderNode.addTradeListener(listener);
	}

	@Override
	public void removeListener(ITradeListener listener) {
		traderNode.removeListener(listener);
	}
}
