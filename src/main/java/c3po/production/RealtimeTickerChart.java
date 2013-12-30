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

	private final static long interpolationTime = 8 * Time.SECONDS;
	private final static long timestep = 4 * Time.SECONDS;
	
	private static final int NUM_PERCENTILES = 6;

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
					orderBook.getOutputP99Bid(),
					orderBook.getOutputP98Bid(),
					orderBook.getOutputP97Bid(),
					orderBook.getOutputP96Bid(),
					orderBook.getOutputP95Bid(),
					ticker.getOutputAsk(),
					orderBook.getOutputP99Ask(),
					orderBook.getOutputP98Ask(),
					orderBook.getOutputP97Ask(),
					orderBook.getOutputP96Ask(),
					orderBook.getOutputP95Ask()
					);
			
			tickerGraph.setLineColor(0, 0.66f, 1f, 1f);
			
			for (int i = 0; i < NUM_PERCENTILES; i++) {
				int index = 1 + i;
				float brightness = 1f  - (i / (float)NUM_PERCENTILES) * 0.5f;
				tickerGraph.setLineColor(index, 0.00f, 1f, brightness);
			}
			
			for (int i = 0; i < NUM_PERCENTILES; i++) {
				int index = 1 + NUM_PERCENTILES + i;
				float brightness = 1f  - (i / (float)NUM_PERCENTILES) * 0.5f;
				tickerGraph.setLineColor(index, 0.33f, 1f, brightness);
			}
			
			tickerGraph.setMaximumItemAge(2 * Time.HOURS);
			tickerGraph.pack();
			tickerGraph.setVisible(true);
			
			GraphingNode volumeGraph = new GraphingNode(timestep, "Volume",
					ticker.getOutputVolume()
					);			
			volumeGraph.setMaximumItemAge(2 * Time.HOURS);
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
