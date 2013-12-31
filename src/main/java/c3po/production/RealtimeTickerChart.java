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

public class RealtimeTickerChart {
	private static final Logger LOGGER = LoggerFactory.getLogger(RealtimeBotRunner.class);

	private final static long interpolationTime = 10 * Time.SECONDS;
	private final static long timestep = 5 * Time.SECONDS;
	private final static long timespan = 4 * Time.HOURS;
	private static final int NUM_PERCENTILES = 10;

	public static void main(String[] args) {
		 
		try {
			// Set up global signal tree
			final BitstampTickerSource ticker = new BitstampTickerJsonSource(timestep, interpolationTime, "https://www.bitstamp.net:443/api/ticker/");
			final BitstampOrderBookSource orderBook = new BitstampOrderBookJsonSource(timestep, interpolationTime, "https://www.bitstamp.net:443/api/order_book/");

			// Create a clock
			IRealtimeClock clock = new RealtimeClock(timestep, 0, interpolationTime);
			
			GraphingNode tickerGraph = new GraphingNode(timestep, "Ticker",
					ticker.getOutputLast(),
					ticker.getOutputBid(),
					orderBook.getOutputBidPercentile(0),
					orderBook.getOutputBidPercentile(1),
					orderBook.getOutputBidPercentile(2),
					orderBook.getOutputBidPercentile(3),
					orderBook.getOutputBidPercentile(4),
					orderBook.getOutputBidPercentile(5),
					orderBook.getOutputBidPercentile(6),
					orderBook.getOutputBidPercentile(7),
					orderBook.getOutputBidPercentile(8),
					ticker.getOutputAsk(),
					orderBook.getOutputAskPercentile(0),
					orderBook.getOutputAskPercentile(1),
					orderBook.getOutputAskPercentile(2),
					orderBook.getOutputAskPercentile(3),
					orderBook.getOutputAskPercentile(4),
					orderBook.getOutputAskPercentile(5),
					orderBook.getOutputAskPercentile(6),
					orderBook.getOutputAskPercentile(7),
					orderBook.getOutputAskPercentile(8)
					);
			
			// Set Last signal to blue
			tickerGraph.setLineColor(0, 0.66f, 1f, 1f);
			
			// Set Ask percentiles to shades of red
			for (int i = 0; i < NUM_PERCENTILES; i++) {
				int index = 1 + i;
				float brightness = 1f  - (i / (float)NUM_PERCENTILES) * 0.5f;
				tickerGraph.setLineColor(index, 0.00f, 1f, brightness);
			}
			
			// Set Bid percentiles to shades of green
			for (int i = 0; i < NUM_PERCENTILES; i++) {
				int index = 1 + NUM_PERCENTILES + i;
				float brightness = 1f  - (i / (float)NUM_PERCENTILES) * 0.5f;
				tickerGraph.setLineColor(index, 0.33f, 1f, brightness);
			}
			
			tickerGraph.setMaximumItemAge(timespan);
			tickerGraph.pack();
			tickerGraph.setVisible(true);
			
			GraphingNode volumeGraph = new GraphingNode(timestep, "Volume",
					ticker.getOutputVolume()
					);			
			volumeGraph.setMaximumItemAge(timespan);
			volumeGraph.pack();
			volumeGraph.setVisible(true);
			
			// Run the program
			
			clock.addListener(volumeGraph);
			clock.addListener(tickerGraph);
			clock.run();
			
		} catch (Exception e) {
			LOGGER.error("Critical error in main", e);
		}
	}
}
