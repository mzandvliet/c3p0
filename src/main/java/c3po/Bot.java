package c3po;

import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import c3po.macd.MacdBotConfig;
import c3po.macd.MacdNodeConfig;

/* Todo:
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
 * 		- Make time range and sampling/tick rate configurable
 * 		- Start using interpolation to correct sample timing error
 * 		- Supply tick() with timestamp of either real time or simulation time, instead of an arbitrary value
 * 
 * - Use a charting library to show results, either live or after a simulation
 * 		- Either implement charts as leaf nodes in the signal tree, or point them to leafs in the tree
 * 		- http://www.jfree.org/jfreechart/samples.html
 * 		- https://code.google.com/p/charts4j/
 */

public class Bot {
	private static final Logger LOGGER = LoggerFactory.getLogger(Bot.class);
	final static String jsonUrl = "http://www.bitstamp.net/api/ticker/";
	final static String csvPath = "resources/bitstamp_ticker_until_20131114.csv";
	
	final long updateInterval;
	final boolean isRealtime;
	final int simulationTicks;
	final double walletDollarStart;

	private final BitstampTickerCsvSource tickerNode;
	
	// Some object references to make debugging easier
	private final MacdBot macdBot;
	private final MacdBotConfig macdBotConfig;
	private final MacdNode macdNode;
	private final MacdNodeConfig macdNodeConfig;
	
	private ITradeFloor tradeFloor;

	
	public static void main(String[] args) throws ClassNotFoundException, SQLException {
		Bot bot = new Bot(false, 14000, 1000.0, 1000);
		bot.run();
	}
	
	public Bot(boolean isRealtime, int simulationTicks, double walletDollarStart, long updateInterval) {
		this.isRealtime = isRealtime;
		this.simulationTicks = simulationTicks;
		this.walletDollarStart = walletDollarStart;
		this.updateInterval = updateInterval;
		
		// Define the signal tree		
		
		//final ISignalSource tickerSource = new BitstampTickerJsonSource(jsonUrl);
		//final BitstampTickerDbSource dbTickerSource = new BitstampTickerDbSource(new InetSocketAddress("94.208.87.249", 3309), "c3po", "D7xpJwzGJEWf5qWB");
		tickerNode = new BitstampTickerCsvSource(csvPath);
		tradeFloor = new BitstampTradeFloor(
				tickerNode.getOutputLast(),
				tickerNode.getOutputBid(),
				tickerNode.getOutputAsk(),
				walletDollarStart
		);
		
		// MacdNodeConfig
		int fast = (int) Math.round(1 + Math.random() * 120); // Default: 12
		int slow = (int) Math.round(1 + Math.random() * 260); // Default: 26
		int signal = (int) Math.round(1 + Math.random() * 200); // Default: 9
		macdNodeConfig = new MacdNodeConfig(slow, fast, signal);
		macdNode = new MacdNode(tickerNode.getOutputLast(), macdNodeConfig);
		
		// MacdBot
		double minDiffVelocity = 0.01 + Math.random() * 0.99; // Default 0.5
		int startDelay = 20; // Default 20
		double usdToBtcTradeAmount = 0.01 + Math.random() * 0.99; // Default 0.5
		double btcToUsdTradeAmount = 0.01 + Math.random() * 0.99; // Default 0.5
		macdBotConfig = new MacdBotConfig(startDelay, minDiffVelocity, usdToBtcTradeAmount, btcToUsdTradeAmount);
		macdBot = new MacdBot(macdNode.getOutput(4), tradeFloor, macdBotConfig);
	}
	
	 
	
	public void run() {
		// Tick the leafs repeatedly to propagate (or 'draw') samples through the tree from roots to leaves
		
		tickerNode.open();
		for (long tick = 0; tick < simulationTicks; tick++) {
			macdBot.tick(tick);
			
			if (isRealtime)
				Wait(updateInterval);
		}
		tickerNode.close();
		
		// Display the results
		
		LOGGER.debug("Num trades: " + tradeFloor.getActions().size() + ", Profit: " + (tradeFloor.getWalletValue() - walletDollarStart));
	}
	
	public String toString() {
		return String.format("%s, %s", macdNodeConfig, macdBotConfig);
	}
	
	private static void Wait(long interval) {
		try {
			Thread.sleep(interval);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public ITradeFloor getTradeFloor() {
		return tradeFloor;
	}

	public void setTradeFloor(ITradeFloor tradeFloor) {
		this.tradeFloor = tradeFloor;
	}
	
	
}
