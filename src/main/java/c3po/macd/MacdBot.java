package c3po.macd;

import c3po.*;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import c3po.BitstampTickerCsvSource;
import c3po.BitstampTradeFloor;
import c3po.IClock;
import c3po.ITradeFloor;

/* Todo:
 * 
 * 
 * - bots should define their own update frequency
 * 
 * 
 * - Algorithms
 * 		- Verify correct macd results (hard to see without charts)
 * 		- Tweak macdBot configuration for profit
 * 
 * - Network architecture
 * 		- Make Sample generic?
 * 			- So you can have ISignal<TradeAction> & INode<TradeAction>
 * 			- Analyse wallet & trade data streams using network methods too
 * 		- Find abstractions that reduce boilerplate for data transformation nodes
 * 			- Something akin to delegates/lambdas, you know
 * 		- Make node input/output indexing more human readable with enums
 * 
 * - Improve TradeFloor interface with
 * 		- Currency abstraction
 * 		- Costs
 * 
 * - Time
 * 		- Timestep parameter is part of bot, integrate it
 * 			- May their rates differ? Can they differ from the source signal?
 * 		- Make time range and sampling/tick rate configurable
 * 		- Start using interpolation to correct sample timing error
 * 		- Supply tick() with timestamp of either real time or simulation time, instead of an arbitrary value
 * 
 * - Use a charting library to show results, either live or after a simulation
 * 		- Either implement charts as leaf nodes in the signal tree, or point them to leafs in the tree
 * 		- http://www.jfree.org/jfreechart/samples.html
 * 		- https://code.google.com/p/charts4j/
 */

public class MacdBot implements IBot {
	//================================================================================
    // Static Properties
    //================================================================================
	
	private static final Logger LOGGER = LoggerFactory.getLogger(MacdBot.class);
	private final static String jsonUrl = "http://www.bitstamp.net/api/ticker/";
	private final static String csvPath = "resources/bitstamp_ticker_until_20131114.csv";
	private final static long simulationSteps = 1400;
	private static final long timeStep = 1000;
	private static final double walletDollarStart = 1000.0;
	
	//================================================================================
    // Main
    //================================================================================
	
	public static void main(String[] args) throws ClassNotFoundException, SQLException {
		// Set up global signal tree
		
		//final ISignalSource tickerSource = new BitstampTickerJsonSource(jsonUrl);
		//final BitstampTickerDbSource dbTickerSource = new BitstampTickerDbSource(new InetSocketAddress("94.208.87.249", 3309), "c3po", "D7xpJwzGJEWf5qWB");
		final BitstampTickerCsvSource tickerNode = new BitstampTickerCsvSource(csvPath);
				
		final ITradeFloor tradeFloor =  new BitstampTradeFloor(
				tickerNode.getOutputLast(),
				tickerNode.getOutputBid(),
				tickerNode.getOutputAsk(),
				walletDollarStart
		);
		
		// Create bot config
		
		MacdAnalysisConfig analysisConfig = new MacdAnalysisConfig(48,102,36); // Todo: trader.startDelay is proportional to this, maybe Max(fast,slow,signal)
		MacdTraderConfig traderConfig = new MacdTraderConfig(102, 0.5, 0.5, 0.5);
		MacdBotConfig config = new MacdBotConfig(timeStep, analysisConfig, traderConfig);
		
		// Create bot
		
		IBot bot = new MacdBot(config, tickerNode.getOutputLast(), tradeFloor);
		
		// Create a clock
		
		IClock botClock = new SimulationClock(config.timeStep, simulationSteps); // Todo: is this part of the bot, or outside of it?
		botClock.addListener(bot);
		
		// Run the program
		
		tickerNode.open();
		
		botClock.run();
		
		tickerNode.close();
		
		// Log results
		
		LOGGER.debug("Num trades: " + tradeFloor.getActions().size() + ", Profit: " + (tradeFloor.getWalletValue() - walletDollarStart));
	}
	
	
	//================================================================================
    // Properties
    //================================================================================
	
    // Debug references
	
	private final MacdAnalysisNode analysisNode;
	private final MacdTraderNode traderNode;
	

	//================================================================================
    // Methods
    //================================================================================
	
	public MacdBot(MacdBotConfig config, ISignal ticker, ITradeFloor tradeFloor) {	
		
		// Define the signal tree		
		
		analysisNode = new MacdAnalysisNode(ticker, config.analysisConfig);
		traderNode = new MacdTraderNode(analysisNode.getOutput(4), tradeFloor, config.traderConfig);
	}
	
	public void tick(long tick) {
		traderNode.tick(tick);
	}
	
	public String toString() {
		return String.format("%s, %s", analysisNode.getConfig(), traderNode.getConfig());
	}
}
