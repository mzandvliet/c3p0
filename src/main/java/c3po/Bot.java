package c3po;

import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* Todo:
 * 
 * - Find a simpler, better abstraction for signals
 * 		- A signal provides one or more sample streams
 * 		- You can split & merge signals
 * - Encapsulate data transformations as nodes in the tree, supporting
 * 		- 1 Signal to 1 Signal (scaling by a constant, or something)
 * 		- 2 Signals to 1 Signal (substraction)
 * 		- 1 Signal to N Signals (macd & other complex indicators)
 * - Create ITradeFloor interface (with a better name perhaps?)
 * 		- basic buy/sell methods
 * 		- wallet supporting multiple currencies
 * 		- pass instance to bot nodes so they can trade
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
	
	final static int simulationTicks = 100;
	final static int resultBufferLength = 20;
	
	public static void main(String[] args) throws ClassNotFoundException, SQLException {
		
		// Define the signal tree		
		
		//final ISignalSource tickerSource = new BitstampTickerJsonSource(jsonUrl);
		//final BitstampTickerDbSource dbTickerSource = new BitstampTickerDbSource(new InetSocketAddress("94.208.87.249", 3309), "c3po", "D7xpJwzGJEWf5qWB");
		final BitstampTickerCsvSource tickerNode = new BitstampTickerCsvSource(csvPath);
		final ISignalBuffer tickerBufferNode = new SignalBuffer(tickerNode.getOutput(2), resultBufferLength);
		final INode emaNode = new ExpMovingAverageNode(tickerBufferNode, 5);
		final ISignalBuffer smoothBuffer = new SignalBuffer(emaNode.getOutput(0), resultBufferLength);
		
		tickerNode.open();
		
		// Tick the leafs repeatedly to propagate (or 'draw') samples through the tree from roots to leaves
		
		for (long tick = 0; tick < simulationTicks; tick++) {
			smoothBuffer.tick(tick);
			
			if (isRealtime)
				Wait(updateInterval);
		}
		
		tickerNode.close();
		
		// Display the results
		
		LOGGER.debug("buffer contents: " + tickerBufferNode.size());
		for (int i = 0; i < tickerBufferNode.size(); i++) {
			LOGGER.debug(tickerBufferNode.get(i).toString() + smoothBuffer.get(i).toString());
		}
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
