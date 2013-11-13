package c3po;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import c3po.TradeAction.TradeActionType;

public class Bot {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(Bot.class);
	
	/**
	 * Scenario's of running
	 * 
	 * We want to run the bot in realtime mode. It needs to fetch new data every x times and analyse if it wants to do something every y time
	 * We want to run the bot in historic mode, taking data between T1 and T2 and analyse when Y time has been passed between the sample timestamps.
	 */


	
	// config
	// granulariteit van data
    // frequency van mogelijk handelen / analyse momenten
    
	
	/*
	// time, buy/sell, volume
	private List<TradeAction> actions;
	
	// Wallet
	private Currency eur;
	private Currency bc;
	
	// state
	boolean realtimeOrHistoric?
    starttime and endtime?	
	
	// wallet transactions / history
*/
	
	
	
	
	
	
	
	
	
	public static void main(String[] args) {

		TradeAction action = new TradeAction(TradeActionType.BUY, 1384327950000l, 123.44);
		
		LOGGER.debug(action.toString());
		/*
	
		while() {
			variation on config
			
			// do the bot
			
			// store the score	
		}
		
		
		// List of components with configuration
		SignalSource ticker = new TickerSource(1);
		Macd macd = new Macd(12,26,9);
		
		
		
		
		ticker.onUpdate += macd.onUpdate;
		
		
		MacdBot bot = new MacdBot(macd, tradeInterface);
		
		while () {
			ticker.update();	
		}
		*/
	}
	
	/*
	public timestamp, value, azimuth, elevation, longitude, latitude  3d(timestamp, a, b, c, d) {
		
	}
		
	public void run() {	
		// Fetch raw data
		SignalSource ticker = new TickerSource(1);
		ticketData = ticker.update();
		
		// Decide what do do
		TradeActions actions =
				
		// Write results in log and database
		
	    // Update graph / interface
	}
	*/
}
