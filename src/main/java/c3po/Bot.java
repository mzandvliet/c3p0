package c3po;

import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import c3po.TradeAction.TradeActionType;

/*
 *  Todo: 
 *  - Support different update rates for every node in the network?
 *  - Maybe
 */

public class Bot {
	private static final Logger LOGGER = LoggerFactory.getLogger(Bot.class);
	
	final static long updateInterval = 1000;
	final static String csvPath = "E:\\code\\workspace\\c3po\\marketdata\\bitstamp_ticker_13-11-13.csv";
	final static boolean isRealtime = false;
	
	public static void main(String[] args) {
		//final ISignalSource tickerSource = new BitstampTickerJsonSource();
		final BitstampTickerCsvSource tickerSource = new BitstampTickerCsvSource(csvPath);
		tickerSource.open();
		
		final ISignalBuffer tickerBuffer = new SignalBuffer(tickerSource, 100);
		
		// Hmmm, hardly the most elegant. If only we had proper delegates...
		// Maybe transformer should be its own node with its own little kernel-sized buffer instead of piggybacking on a regular SignalBuffer
		// Keep the idea of injected transform method though, saves on boilerplate.
		// Actually, making this a node is best because the transformation becomes more explicit, more visible
		final ISignalTransformer movAvgTransformer = new ISignalTransformer() {
			private int kernelSize = 5;
			@Override
			public Signal transform(List<Signal> lastSignals, Signal newest) {
				return Indicators.filterMovingAverage(lastSignals, newest, kernelSize);
			}
		};
		
		final ISignalBuffer smoothBuffer = new SignalBuffer(tickerBuffer, 100, movAvgTransformer);
		
		for (long tick = 0; tick < 100; tick++) {
			smoothBuffer.getLatest(tick);
			
			if (isRealtime)
				Wait(updateInterval);
		}
		
		LOGGER.debug("buffer contents: " + tickerBuffer.size());
		for (int i = 0; i < tickerBuffer.size(); i++) {
			LOGGER.debug(tickerBuffer.get(i).toString() + smoothBuffer.get(i).toString());
		}
		
		tickerSource.close();
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
