package c3po;

import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import c3po.TradeAction.TradeActionType;

public class Bot {
	private static final Logger LOGGER = LoggerFactory.getLogger(Bot.class);
	
	final static long updateInterval = 1000;
	
	public static void main(String[] args) {
		final ISignalSource tickerSource = new BitstampTickerSignalSource();
		final ISignalBuffer tickerBuffer = new SignalBuffer(tickerSource, 10);
		
		for (long tick = 0; tick < 20; tick++) {
			Signal current = tickerBuffer.getLatest(tick);
						
			try {
				Thread.sleep(updateInterval);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		LOGGER.debug("buffer contents");
		for (int i = 0; i < tickerBuffer.size(); i++) {
			LOGGER.debug(tickerBuffer.get(i).toString());
		}
	}
}
