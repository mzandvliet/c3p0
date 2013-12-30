package c3po.production;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import c3po.bitstamp.BitstampOrderBookJsonSource;
import c3po.bitstamp.BitstampOrderBookSource;
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
			final BitstampTickerSource tickerNode = new BitstampTickerJsonSource(timestep, interpolationTime, "https://www.bitstamp.net:443/api/ticker/");
			final BitstampOrderBookSource orderBookNode = new BitstampOrderBookJsonSource(timestep, interpolationTime, "https://www.bitstamp.net:443/api/order_book/");

			// Create a clock
			IRealtimeClock botClock = new RealtimeClock(timestep, 0, interpolationTime);
			
			GraphingNode tickerGraph = new GraphingNode(timestep, "Ticker",
					tickerNode.getOutputLast(),
					orderBookNode.getOutputP99Bid(),
					orderBookNode.getOutputP98Bid(),
					orderBookNode.getOutputP96Bid(),
					orderBookNode.getOutputP99Ask(),
					orderBookNode.getOutputP98Ask(),
					orderBookNode.getOutputP96Ask()
					);			
			tickerGraph.setMaximumItemAge(2 * Time.HOURS);
			tickerGraph.pack();
			tickerGraph.setVisible(true);
			botClock.addListener(tickerGraph);
			
			GraphingNode volumeGraph = new GraphingNode(timestep, "Volume",
					tickerNode.getOutputVolume()
					);			
			volumeGraph.setMaximumItemAge(2 * Time.HOURS);
			volumeGraph.pack();
			volumeGraph.setVisible(true);
			botClock.addListener(volumeGraph);
			
			// Run the program
			botClock.run();
			
		} catch (Exception e) {
			LOGGER.error("Critical error in main", e);
		}
	}
}
