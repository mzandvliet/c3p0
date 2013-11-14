package c3po;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* Todo:
 * 
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
	
	public static void main(String[] args) {
		
		// Define the signal tree		
		
		//final ISignalSource tickerSource = new BitstampTickerJsonSource(jsonUrl);
		final BitstampTickerCsvSource tickerSource = new BitstampTickerCsvSource(csvPath);
		final ISignalBuffer tickerBuffer = new SignalBuffer(tickerSource.get(2), resultBufferLength);
		final MovingAverageSignal maSignal = new MovingAverageSignal(tickerBuffer, 5);
		final ISignalBuffer smoothBuffer = new SignalBuffer(maSignal, resultBufferLength);
		
		tickerSource.open();
		
		// Tick the leafs repeatedly to propagate (or 'draw') signals through the tree from roots to leaves
		
		for (long tick = 0; tick < simulationTicks; tick++) {
			smoothBuffer.tick(tick);
			
			if (isRealtime)
				Wait(updateInterval);
		}
		
		tickerSource.close();
		
		// Display the results
		
		LOGGER.debug("buffer contents: " + tickerBuffer.size());
		for (int i = 0; i < tickerBuffer.size(); i++) {
			LOGGER.debug(tickerBuffer.get(i).toString() + smoothBuffer.get(i).toString());
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
