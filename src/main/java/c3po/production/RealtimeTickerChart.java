package c3po.production;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import c3po.bitstamp.BitstampTickerJsonSource;
import c3po.bitstamp.BitstampTickerSource;
import c3po.clock.IRealtimeClock;
import c3po.node.GraphingNode;
import c3po.utils.Time;

/* TODO: Set a fixed data range, so it grow until a memory crash, obviously. */

public class RealtimeTickerChart {
	private static final Logger LOGGER = LoggerFactory.getLogger(RealtimeBotRunner.class);

	private final static long interpolationTime = 4 * Time.SECONDS;
	private final static long timestep = 2 * Time.SECONDS;

	public static void main(String[] args) {
		 
		try {
			// Set up global signal tree
			final BitstampTickerSource tickerNode = new BitstampTickerJsonSource(timestep, interpolationTime, "https://www.bitstamp.net/api/ticker/");

			// Create a clock
			IRealtimeClock botClock = new RealtimeClock(timestep, 0, interpolationTime);
			
			GraphingNode tickerGraph = new GraphingNode(timestep, "Ticker",
					tickerNode.getOutputLast(),
					tickerNode.getOutputBid(),
					tickerNode.getOutputAsk()
					);
			botClock.addListener(tickerGraph);
			
			tickerGraph.setMaximumItemAge(60 * 60);
			tickerGraph.pack();
			tickerGraph.setVisible(true);
			
			// Run the program
			tickerNode.open();
			botClock.run();
			tickerNode.close();
			
		} catch (Exception e) {
			LOGGER.error("Critical error in main", e);
		}
	}
}
