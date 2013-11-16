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
	
	private final static long simulationSteps = 1400;
	private static final long timeStep = 1000;
	
	private final static int numBots = 100;
	private final static double walletStartDollars = 1000.0;
	
	public static void main(String[] args) throws ClassNotFoundException, SQLException {
		final BitstampTickerCsvSource tickerNode = new BitstampTickerCsvSource(csvPath);
		
		// Create the bots
		
		for (int i = 0; i < numBots; i++) {
			
			// Each bot needs its own wallet (Todo: But not its own tradefloor!!!)
			
			final ITradeFloor tradeFloor =  new BitstampTradeFloor(
					tickerNode.getOutputLast(),
					tickerNode.getOutputBid(),
					tickerNode.getOutputAsk(),
					walletStartDollars
			);
			
			// Create unique bot config
			MacdAnalysisConfig analysisConfig = new MacdAnalysisConfig(48,102,36);
			MacdTraderConfig traderConfig = new MacdTraderConfig(102, 0.5, 0.5, 0.5);
			MacdBotConfig config = new MacdBotConfig(timeStep, analysisConfig, traderConfig);
			
			IBot bot = new MacdBot(config, tickerNode.getOutputLast(), tradeFloor);
			bots.add(bot);
		}
		
		// Create a clock
		
		IClock botClock = new SimulationClock(simulationSteps, timeStep);
		
		// Run the program
		
		tickerNode.open();
		
		botClock.run();
		
		tickerNode.close();
	}
	
	private static MacdBotConfig Mutate(MacdBotConfig config) {

		// Todo:Mutate properties based on chance
		
		return null;
	}
}
