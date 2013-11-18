package c3po.macd;

import c3po.*;

import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import c3po.BitstampTickerCsvSource;
import c3po.BitstampSimulationTradeFloor;
import c3po.IClock;
import c3po.ITradeFloor;

/* Todo:
 * 
 * ------ Important -----
 * - TickerDBSource and TickerJsonSource should use MILISECONDS!!
 * - MacdAnalysisConfig is still expressed in number-of-ticks, which means it depends on bot's timeStep. Should change to units of time.
 * - Encapsulate tick invalidation, it's so easy to do wrong, and it is a bunch of boilerplate
 * 		- abstract class AbstractTickable implements ITickable { protected abstract void onNewTick(); }
 * 		- That handles the if (tick > last), and just does a callback on your code when new values are needed
 * ----------------------
 * 
 * - Develop exit protocol and implementation
 * - Seed newly started bots with data from recorded history, then update with live feed
 * 
 * - Algorithms
 * 		- Genetic algorithm optimizer
 * 		- Consider variations for opening and closing positions in Macd. See how they affect performance.
 * 			- Ride the wave from beginning to end
 * 			- Spread the wave into multiple positions (like we do now)
 * 			- Do buy/sell in sequence or decoupled (like we do now)
 * 
 * - Time
 * 		- Start using interpolation to correct sample timing error
 * 
 * - Use a charting library to show results, either live or after a simulation
 * 		- Either implement charts as leaf nodes in the signal tree, or point them to leafs in the tree
 * 		- http://www.jfree.org/jfreechart/samples.html
 * 		- https://code.google.com/p/charts4j/
 * 
 * - Network architecture
 * 		- INode should define setInput() to assign signals dynamically
 * 			- Constructor should not take input signals anymore
 * 		- Make Sample generic?
 * 			- So you can have ISignal<TradeAction> & INode<TradeAction>
 * 			- Analyse wallet & trade data streams using network methods too
 * 		- Find abstractions that reduce boilerplate for data transformation nodes
 * 			- Something akin to delegates/lambdas, you know
 * 		- Make node input/output indexing more human readable with enums
 * 
 * - Improve TradeFloor interface
 * 		- Currency abstraction
 * 		- Separate wallet concept from TradeFloor
 * 		- Costs
 * 
 */

public class MacdBot implements IBot {
	//================================================================================
    // Static Properties
    //================================================================================
	
	private static final Logger LOGGER = LoggerFactory.getLogger(MacdBot.class);
	private final static String jsonUrl = "http://www.bitstamp.net/api/ticker/";
	private final static String csvPath = "resources/bitstamp_ticker_fake_down_up.csv";
	private final static long simulationStartTime = 1383468287000l;
	private final static long simulationEndTime = 1384689637000l; 
	
	private final static long clockTimestep = 1000;
	private static final long botTimestep = 1000;
	private static final double walletDollarStart = 1000.0;
	private static final double walletBtcStart = 0.0;
	
	//================================================================================
    // Main
    //================================================================================
	
	public static void main(String[] args) throws ClassNotFoundException, SQLException {
		// Set up global signal tree
		
		//final ISignalSource tickerSource = new BitstampTickerJsonSource(jsonUrl);
		//final BitstampTickerDbSource dbTickerSource = new BitstampTickerDbSource(new InetSocketAddress("94.208.87.249", 3309), "c3po", "D7xpJwzGJEWf5qWB");
		final BitstampTickerCsvSource tickerNode = new BitstampTickerCsvSource(csvPath);
				
		final ITradeFloor tradeFloor =  new BitstampSimulationTradeFloor(
				tickerNode.getOutputLast(),
				tickerNode.getOutputBid(),
				tickerNode.getOutputAsk(),
				walletDollarStart,
				walletBtcStart
		);
		
		// Create bot config
		
		// todo: this is still in number-of-ticks, which means it depends on bot's timeStep, should change to units of time
		
		MacdAnalysisConfig analysisConfig = new MacdAnalysisConfig(52,24,18); // Todo: trader.startDelay is proportional to this, maybe Max(fast,slow,signal)
		MacdTraderConfig traderConfig = new MacdTraderConfig(102, 0.15, 0.5 , 0.5, 0.5, 600000, 600000);
		MacdBotConfig config = new MacdBotConfig(botTimestep, analysisConfig, traderConfig);
		
		// Create bot
		
		IBot bot = new MacdBot(config, tickerNode.getOutputLast(), tradeFloor);
		
		// Create a clock
		
		IClock botClock = new SimulationClock(clockTimestep, simulationStartTime, simulationEndTime);
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
	
	private final MacdBotConfig config;
	private final ITradeFloor tradeFloor;
	
	private long lastTick;
	
    // Debug references
	
	private final MacdAnalysisNode analysisNode;
	private final MacdTraderNode traderNode;
	

	//================================================================================
    // Methods
    //================================================================================
	
	public MacdBot(MacdBotConfig config, ISignal ticker, ITradeFloor tradeFloor) {	
		this.config = config;
		this.tradeFloor = tradeFloor;
		
		// Define the signal tree		
		
		analysisNode = new MacdAnalysisNode(ticker, config.analysisConfig);
		traderNode = new MacdTraderNode(analysisNode.getOutput(4), tradeFloor, config.traderConfig);
	}
	
	@Override
	public long getLastTick() {
		return lastTick;
	}
	
	@Override
	public void tick(long tick) {
		if (tick > lastTick) {
			traderNode.tick(tick);
		}
		lastTick = tick;
	}
	
	@Override
	public long getTimestep() {
		return config.timeStep;
	}

	@Override
	public ITradeFloor getTradeFloor() {
		return tradeFloor;
	}
	
	public MacdBotConfig getConfig() {
		return config;
	}

	public String toString() {
		return String.format("%s, %s", analysisNode.getConfig(), traderNode.getConfig());
	}
}
