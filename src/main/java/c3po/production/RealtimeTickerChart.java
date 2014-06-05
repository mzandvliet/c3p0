package c3po.production;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import c3po.bitstamp.BitstampOrderBookJsonSource;
import c3po.bitstamp.BitstampTickerJsonSource;
import c3po.bitstamp.BitstampTickerSource;
import c3po.clock.IRealtimeClock;
import c3po.node.GraphingNode;
import c3po.orderbook.IOrderBookSource;
import c3po.orderbook.OrderBookPercentileTransformer;
import c3po.orderbook.OrderBookVolumePercentileTransformer;
import c3po.utils.Time;

public class RealtimeTickerChart {
	private static final Logger LOGGER = LoggerFactory.getLogger(RealtimeBotRunner.class);

	private final static long interpolationTime = 10 * Time.SECONDS;
	private final static long timestep = 5 * Time.SECONDS;
	private final static long timespan = 4 * Time.HOURS;
	
	private static final double[] percentiles = { 99.5, 99.0, 98.5, 98.0, 97.5 , 97.0, 96.5, 96, 95.5, 95 };

	public static void main(String[] args) {
		 
		try {
			// Set up global signal tree
			final BitstampTickerSource ticker = new BitstampTickerJsonSource(timestep, interpolationTime, "https://www.bitstamp.net:443/api/ticker/");
//			final IOrderBookSource orderBook = new BitstampOrderBookJsonSource(timestep, "https://www.bitstamp.net:443/api/order_book/");
//			final OrderBookPercentileTransformer percentileTransformer = new OrderBookVolumePercentileTransformer(timestep, interpolationTime, percentiles, orderBook);

			// Create a clock
			IRealtimeClock clock = new RealtimeClock(timestep, 0, interpolationTime);

			GraphingNode tickerGraph = new GraphingNode(timestep, "Ticker",
					ticker.getOutputLast(),
					ticker.getOutputBid(),
//					percentileTransformer.getOutputBidPercentile(0),
//					percentileTransformer.getOutputBidPercentile(1),
//					percentileTransformer.getOutputBidPercentile(2),
//					percentileTransformer.getOutputBidPercentile(3),
//					percentileTransformer.getOutputBidPercentile(4),
//					percentileTransformer.getOutputBidPercentile(5),
//					percentileTransformer.getOutputBidPercentile(6),
//					percentileTransformer.getOutputBidPercentile(7),
//					percentileTransformer.getOutputBidPercentile(8),
//					percentileTransformer.getOutputBidPercentile(9),
					ticker.getOutputAsk()
//					percentileTransformer.getOutputAskPercentile(0)
//					percentileTransformer.getOutputAskPercentile(1),
//					percentileTransformer.getOutputAskPercentile(2),
//					percentileTransformer.getOutputAskPercentile(3),
//					percentileTransformer.getOutputAskPercentile(4),
//					percentileTransformer.getOutputAskPercentile(5),
//					percentileTransformer.getOutputAskPercentile(6),
//					percentileTransformer.getOutputAskPercentile(7),
//					percentileTransformer.getOutputAskPercentile(8),
//					percentileTransformer.getOutputAskPercentile(9)
				);
			
			// Set Last signal to blue
			tickerGraph.setLineColor(0, 0f, 0f, 1f);
			tickerGraph.setLineColor(1, 0f, 1f, 0f);
			tickerGraph.setLineColor(2, 1f, 0f, 0f);
			
//			int numPercentiles = 1 + percentiles.length; // Include P100, which isn't in the list right now
//			
//			// Set Ask percentiles to shades of red
//			for (int i = 0; i < numPercentiles; i++) {
//				int index = 1 + i;
//				float brightness = 1f  - (i / (float)numPercentiles) * 0.75f;
//				tickerGraph.setLineColor(index, brightness, 0f, 0f);
//			}
//			
//			// Set Bid percentiles to shades of green
//			for (int i = 0; i < numPercentiles; i++) {
//				int index = 1 + numPercentiles + i;
//				float brightness = 1f  - (i / (float)numPercentiles) * 0.75f;
//				tickerGraph.setLineColor(index, 0f, brightness, 0f);
//			}
			
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
