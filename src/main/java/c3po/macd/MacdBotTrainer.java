package c3po.macd;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import c3po.BitstampTickerCsvSource;
import c3po.BitstampTradeFloor;
import c3po.IBot;
import c3po.IClock;
import c3po.ITradeFloor;
import c3po.SimulationClock;

/**
 * This class is used to repeatedly run a bit with different configurations and analyse its performance
 * 
 * Todo:
 * 
 * - Create config mutation algorithms
 * 
 * - Genetic algoritm routine:
 * 
 * - Simulate epoch
 * - Select epoch winners
 * - Mutate winners
 * - Repeat until local optimum is reached
 */

public class MacdBotTrainer {
	private static final Logger LOGGER = LoggerFactory.getLogger(MacdBot.class);
	private final static String csvPath = "resources/bitstamp_ticker_until_20131114.csv";
	
	private final static List<IBot> bots = new LinkedList<IBot>();
	
	private final static long simulationStartTime = 1384079023;
	private final static long simulationEndTime = 1384412693;
	private final static long clockTimestep = 1;
	
	private final static int numBots = 100;
	private final static double walletStartDollars = 1000.0;
	
	public static void main(String[] args) throws ClassNotFoundException, SQLException {
		// Todo loop and mutate
		simulateEpoch();
	}
	
	private static void simulateEpoch() {
		// Create a ticker
		
		final BitstampTickerCsvSource tickerNode = new BitstampTickerCsvSource(csvPath);
		
		// Create a clock
		
		IClock botClock = new SimulationClock(clockTimestep, simulationStartTime, simulationEndTime);
		
		// Create the bots
		
		for (int i = 0; i < numBots; i++) {
			
			// Todo: bots should have a personal wallet, but could share a tradeFloor. Split wallet and tradefloor concepts
			
			final ITradeFloor tradeFloor =  new BitstampTradeFloor(
					tickerNode.getOutputLast(),
					tickerNode.getOutputBid(),
					tickerNode.getOutputAsk(),
					walletStartDollars
			);
			
			// Create unique bot config (todo: randomize/mutate)
			MacdAnalysisConfig analysisConfig = new MacdAnalysisConfig(48,102,36);
			MacdTraderConfig traderConfig = new MacdTraderConfig(102, 0.5, 0.5, 0.5);
			MacdBotConfig config = new MacdBotConfig(1000, analysisConfig, traderConfig);
			
			IBot bot = new MacdBot(config, tickerNode.getOutputLast(), tradeFloor);
			botClock.addListener(bot);
			bots.add(bot);
		}
		
		
		
		// Run the program
		
		tickerNode.open();
		
		botClock.run();
		
		tickerNode.close();
		
		// Todo: remove bots from clock
	}
	
	private static MacdBotConfig Mutate(MacdBotConfig config) {

		// Todo:Mutate properties based on chance
		
		return null;
	}
}
