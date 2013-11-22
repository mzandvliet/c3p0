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
	
	private final static String csvPath = "resources/bitstamp_ticker_till_20131122.csv";
	private final static long simulationStartTime = 1384079023000l;
	private final static long simulationEndTime = 1385104422000l; 
	private final static long interpolationTime = 120000; // Delay data by two minutes for interpolation
	
	private final static long clockTimestep = 10000;
	private static final long botTimestep = 60000;
	private static final double walletDollarStart = 1000.0;
	private static final double walletBtcStart = 0.0;
	
	//================================================================================
    // Main
    //================================================================================
	
	public static void main(String[] args) throws ClassNotFoundException, SQLException {
		// Set up global signal tree
		
		//final ISignalSource tickerNode = new BitstampTickerJsonSource(jsonUrl);
		//final BitstampTickerDbSource tickerNode = new BitstampTickerDbSource(interpolationTime, new InetSocketAddress("94.208.87.249", 3309), "c3po", "D7xpJwzGJEWf5qWB");
		final BitstampTickerCsvSource tickerNode = new BitstampTickerCsvSource(interpolationTime, csvPath);
			
		final IWallet wallet = new Wallet(walletDollarStart, walletBtcStart);
		
		final ITradeFloor tradeFloor =  new BitstampSimulationTradeFloor(
				tickerNode.getOutputLast(),
				tickerNode.getOutputBid(),
				tickerNode.getOutputAsk()
		);
		
		// Create bot config
		
		// todo: this is still in number-of-ticks, which means it depends on bot's timeStep, should change to units of time
		
		MacdAnalysisConfig analysisConfig = new MacdAnalysisConfig(154,159,392);
		MacdTraderConfig traderConfig = new MacdTraderConfig(0.1274, -0.3628, 0.9717, 0.0299, 76323000, 29541000);
		MacdBotConfig config = new MacdBotConfig(botTimestep, analysisConfig, traderConfig);
		
		// Create the charter
		
//		GraphingNode graph = new GraphingNode(tickerNode.getOutputHigh(), "Ticker");
//		graph.pack();
//		graph.setVisible(true);
		
		// Create bot
		
		MacdBot bot = new MacdBot(config, tickerNode.getOutputHigh(), wallet, tradeFloor);
		
		// Create loggers
		
		DebugTradeLogger tradeLogger = new DebugTradeLogger();
		bot.addListener(tradeLogger);
		
		DbTradeLogger dbTradeLogger = new DbTradeLogger(bot, new InetSocketAddress("94.208.87.249", 3309), "c3po", "D7xpJwzGJEWf5qWB");
		dbTradeLogger.open();
		dbTradeLogger.startSession(simulationStartTime, walletDollarStart, walletBtcStart);
		
		// Create a clock
		
		IClock botClock = new SimulationClock(clockTimestep, simulationStartTime, simulationEndTime, interpolationTime);
		botClock.addListener(bot);
		
		// Run the program
		
		tickerNode.open();
		
		botClock.run();
		
		tickerNode.close();
		
		dbTradeLogger.close();
		
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
		this.config = config;
		this.wallet = wallet;
		this.tradeFloor = tradeFloor;
		
		// Define the signal tree		
		
		analysisNode = new MacdAnalysisNode(ticker, config.analysisConfig);
		traderNode = new MacdTraderNode(analysisNode.getOutput(4), wallet, tradeFloor, config.traderConfig, config.analysisConfig.max());
	}

	@Override
	public void onNewTick(long tick) {
		traderNode.tick(tick);
	}
	
	@Override
	public long getTimestep() {
		return config.timeStep;
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
	public void addListener(ITradeListener listener) {
		traderNode.addListener(listener);
	}

	@Override
	public void removeListener(ITradeListener listener) {
		traderNode.removeListener(listener);
	}
}
