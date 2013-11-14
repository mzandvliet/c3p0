package c3po;

import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import c3po.TradeAction.TradeActionType;

public class Bot {
	private static final Logger LOGGER = LoggerFactory.getLogger(Bot.class);
	
	final static long updateInterval = 1000;
	final static String jsonUrl = "http://www.bitstamp.net/api/ticker/";
	final static String csvPath = "resources/bitstamp_ticker_until_20131114.csv";
	final static boolean isRealtime = true;
	
	public static void main(String[] args) {
		//final ISignalSource tickerSource = new BitstampTickerJsonSource(jsonUrl, 6);
		final BitstampTickerCsvSource tickerSource = new BitstampTickerCsvSource(csvPath, 6);
		tickerSource.open();
		
		final ISignalBuffer tickerBuffer = new SignalBuffer(tickerSource.get(1), 20);
		final ISignalBuffer smoothBuffer = new SignalBuffer(tickerBuffer, 20);
		
		for (long tick = 0; tick < 10; tick++) {
			smoothBuffer.getSample(tick);
			
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
