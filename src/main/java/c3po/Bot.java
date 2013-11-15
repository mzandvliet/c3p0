package c3po;

import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* Todo:
 * 
 * - Algorithms
 * 		- Verify correct macd results (hard to see without charts)
 * 		- Tweak macdBot configuration for profit
 * 
 * - Network architecture
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
 * 
 * - Use a charting library to show results, either live or after a simulation
 * 		- Either implement charts as leaf nodes in the signal tree, or point them to leafs in the tree
 * 		- http://www.jfree.org/jfreechart/samples.html
 * 		- https://code.google.com/p/charts4j/
 */

public class Bot {
	private static final Logger LOGGER = LoggerFactory.getLogger(Bot.class);
	
	final static long updateInterval = 1000;
	final static String jsonUrl = "http://www.bitstamp.net/api/ticker/";
	final static String csvPath = "resources/bitstamp_ticker_until_20131114.csv";
	final static boolean isRealtime = false;
	
	final static int simulationTicks = 1400;
	final static double walletDollarStart = 1000.0;
	
	public static void main(String[] args) throws ClassNotFoundException, SQLException {
		
		// Define the signal tree		
		
		//final ISignalSource tickerSource = new BitstampTickerJsonSource(jsonUrl);
		//final BitstampTickerDbSource dbTickerSource = new BitstampTickerDbSource(new InetSocketAddress("94.208.87.249", 3309), "c3po", "D7xpJwzGJEWf5qWB");
		final BitstampTickerCsvSource tickerNode = new BitstampTickerCsvSource(csvPath);
		final ITradeFloor tradeFloor = new BitstampTradeFloor(
				tickerNode.getOutput(0),
				tickerNode.getOutput(4),
				tickerNode.getOutput(5),
				walletDollarStart);
		
		final INode macdNode = new MacdNode(tickerNode.getOutput(0), 12, 26, 9);
		final MacdBot macdBot = new MacdBot(macdNode.getOutput(4), tradeFloor, 0.5, 20);
		
		// Tick the leafs repeatedly to propagate (or 'draw') samples through the tree from roots to leaves
		
		tickerNode.open();
		for (long tick = 0; tick < simulationTicks; tick++) {
			macdBot.tick(tick);
			
			if (isRealtime)
				Wait(updateInterval);
		}
		tickerNode.close();
		
		// Display the results
		
		LOGGER.debug("Num trades: " + tradeFloor.getActions().size() + ", Profit: " + (tradeFloor.getWalledUsd() - walletDollarStart));
	}
	
	private static void Wait(long interval) {
		try {
			Thread.sleep(interval);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
