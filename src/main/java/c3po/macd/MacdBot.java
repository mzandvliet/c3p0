package c3po.macd;

import c3po.*;

import java.net.InetSocketAddress;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import c3po.bitstamp.BitstampSimulationTradeFloor;
import c3po.IClock;
import c3po.ISignal;
import c3po.ITradeFloor;
import c3po.ITradeListener;
import c3po.IWallet;
import c3po.bitstamp.BitstampTickerCsvSource;

/* Todo:
 * 
 * ------------------------------------
 * 1. FIX CSV CRASH GENERATOR
 * 2. REMOVE ARTIFICIAL SIGNAL CLAMPER IN CSVSOURCE
 * ------------------------------------
 * 
 * - Create day-trader bots
 * 
 * - Implement better polling to mitigate server data misses
 * 
 * 
 * - Bot Design
 * 		- Manage time duration of open positions
 * 			- Build risk into macd with volatility node
 * 			- High volatility means bot should close positions faster to mitigate crash risk
 * 		- Use varying macd configurations to fit current market context
 * 
 * - Develop exit protocol and implementation
 * - Seed newly started bots with data from recorded history, then update with live feed
 * 
 * 
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
	
//	private final static String csvPath = "resources/bitstamp_ticker_till_20131122.csv";
//	private final static long simulationStartTime = 1384079023000l;
//	private final static long simulationEndTime = 1385156429000l; 
	
	private final static String csvPath = "resources/bitstamp_ticker_till_20131122_crashed.csv";
	private final static long simulationStartTime = 1384079023000l;
	private final static long simulationEndTime = 1385192429000l; 
	
	private final static long interpolationTime = 2 * Time.MINUTES; // Delay data by two minutes for interpolation
	
	private final static long timestep = 1 * Time.MINUTES;
	
	private final static double walletDollarStart = 100.0d;
	private final static double walletBtcStart = 0.0d;
	
	private final static long graphInterval = 10 * Time.MINUTES;
	
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
		
		MacdAnalysisConfig analysisConfig = new MacdAnalysisConfig(
				33 * Time.MINUTES,
				34 * Time.MINUTES,
				2 * Time.MINUTES);
		
		MacdTraderConfig traderConfig = new MacdTraderConfig(
				-0.0771,
				1.8221,
				0.5492,
				0.7554,
				1 * Time.MINUTES,
				3 * Time.MINUTES);
		MacdBotConfig config = new MacdBotConfig(timestep, analysisConfig, traderConfig);
		
		// Create bot
		
		MacdBot bot = new MacdBot(config, tickerNode.getOutputLast(), wallet, tradeFloor);
		
		// Create loggers
		
		DebugTradeLogger tradeLogger = new DebugTradeLogger();
		bot.addTradeListener(tradeLogger);
		
		// Create the grapher
		
		GraphingNode grapher = new GraphingNode(graphInterval, "MacdBot", 
				tickerNode.getOutputLast(),
				tickerNode.getOutputBid(),
				tickerNode.getOutputAsk());
		
//		GraphingNode grapher = new GraphingNode(graphInterval, "MacdBot", 
//				tickerNode.getOutputLast(),
//				bot.analysisNode.getOutput(0),
//				bot.analysisNode.getOutput(1)
//				);
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
		tradeFloor.updateWallet(wallet);		
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
