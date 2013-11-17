package c3po.macd;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import c3po.BitstampTickerCsvSource;
import c3po.BitstampTickerSource;
import c3po.BitstampSimulationTradeFloor;
import c3po.IBot;
import c3po.IClock;
import c3po.ISignal;
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
	private static final Logger LOGGER = LoggerFactory.getLogger(MacdBotTrainer.class);
//	private final static String csvPath = "resources/bitstamp_ticker_fake_downhill.csv";
//	private final static long simulationStartTime = 1383468287000l;
//	private final static long simulationEndTime = 1384078962000l; 
	
//	private final static String csvPath = "resources/bitstamp_ticker_fake_down_up.csv";
//	private final static long simulationStartTime = 1383468287000l;
//	private final static long simulationEndTime = 1384689637000l; 
	
	private final static String csvPath = "resources/bitstamp_ticker_till_20131117.csv";
	private final static long simulationStartTime = 1384079023000l;
	private final static long simulationEndTime = 1384689637000l; 
	
	private final static long clockTimestep = 1000;
	
	private final static int numBots = 100;
	private final static double walletStartDollars = 1000.0;
	
	public static void main(String[] args) throws ClassNotFoundException, SQLException {
		MacdBotTrainer trainer = new MacdBotTrainer();
		
		trainer.simulateEpoch();
	}
	
	private void simulateEpoch() {
		// Create a ticker
		
		final BitstampTickerSource tickerNode = new BitstampTickerCsvSource(csvPath);
		
		// Create a clock
		
		IClock botClock = new SimulationClock(clockTimestep, simulationStartTime, simulationEndTime);
		
		// Create the bots

		List<IBot> population = getRandomPopulation(numBots, tickerNode, botClock);
		
		
		
		// Run the program
		
		tickerNode.open();
		
		botClock.run();
		
		tickerNode.close();
		
		// Todo: remove bots from clock
		
		double highestWalletValue = 0;
		IBot bestBot = null;
		for(IBot bot : population) {
			double walletValue = bot.getTradeFloor().getWalletValue();
			
			if(walletValue > highestWalletValue && bot.getTradeFloor().getActions().size() > 0){
				highestWalletValue = walletValue;
				bestBot = bot;
			}
				
		}
		
		LOGGER.debug("Bot " + bestBot + " has " + highestWalletValue + " USD");	
		bestBot.getTradeFloor().dump();
	}
	
	private List<IBot> getRandomPopulation(int size, BitstampTickerSource ticker, IClock botClock) {
		ArrayList<IBot> population = new ArrayList<IBot>();
		
		for (int i = 0; i < size; i++) {
			final ITradeFloor tradeFloor =  new BitstampSimulationTradeFloor(
					ticker.getOutputLast(),
					ticker.getOutputBid(),
					ticker.getOutputAsk(),
					walletStartDollars
			);
			
			MacdBot bot = new MacdBot(getRandom(), ticker.getOutputLast(), tradeFloor);
			botClock.addListener(bot);
			population.add(bot);
		}
		
		return population;
	}
	
	private MacdBotConfig getRandom() {		
		MacdAnalysisConfig analysisConfig = new MacdAnalysisConfig(
			1 + (int) Math.floor(Math.random() * 1000),
			1 + (int) Math.floor(Math.random() * 1000),
			1 + (int) Math.floor(Math.random() * 1000)
		);
		
		double minBuyThreshold = 0.0d + (Math.random() * 1.0d);
		double minSellThreshold = -0.5d + (Math.random() * 1.0d);
		MacdTraderConfig traderConfig = new MacdTraderConfig(
				Math.max(analysisConfig.fastPeriod, Math.max(analysisConfig.signalPeriod, analysisConfig.slowPeriod)),
				minBuyThreshold,
				minSellThreshold,
				Math.random(),
				Math.random(),
				60000l + (long) (Math.random() * 86400000d),
				60000l + (long) (Math.random() * 86400000d)
				
		);
		
		MacdBotConfig config = new MacdBotConfig(1000, analysisConfig, traderConfig);
		
		return config;
	}
	
	private MacdBotConfig mutate(final MacdBotConfig config) {

		// Todo:Mutate properties based on chance
		
		return null;
	}
}
