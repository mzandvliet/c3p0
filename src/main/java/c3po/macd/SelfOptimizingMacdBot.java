package c3po.macd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import c3po.BitstampSimulationTradeFloor;
import c3po.BitstampTickerCsvSource;
import c3po.IBot;
import c3po.ISignal;
import c3po.ITradeFloor;

public class SelfOptimizingMacdBot implements IBot {
	
private static final Logger LOGGER = LoggerFactory.getLogger(MacdBotTrainer.class);
	private final static String csvPath = "resources/bitstamp_ticker_till_20131117_pingpong.csv";
	private final static long simulationStartTime = 1384079023000l;
	private final static long simulationEndTime = 1384682984000l; 
	
	private final static long clockTimestep = 10000;
	private final static long botStepTime = 60000; // Because right now we're keeping it constant, and data sampling rate is ~1 minute
	

	private final static int numEpochs = 100;
	private final static int numBots = 100;
	
	private final static int numParents = 50;
	private final static int numElites = 5;
	private final static double mutationChance = 0.33d;
	
	private final static double walletStartDollars = 1000.0;
	
	//================================================================================
    // Main
    //================================================================================

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		final BitstampTickerCsvSource tickerNode = new BitstampTickerCsvSource(csvPath);
		
		final ITradeFloor tradeFloor =  new BitstampSimulationTradeFloor(
				tickerNode.getOutputLast(),
				tickerNode.getOutputBid(),
				tickerNode.getOutputAsk(),
				walletStartDollars,
				0.0
		);
	}
	
	//================================================================================
    // Class
    //================================================================================

	public SelfOptimizingMacdBot(ISignal ticker, ITradeFloor tradefloor, int traderWindow, int optimizerWindow) {
		/*
		 *  Workings:
		 *  
		 *  - Create internal MacdBot that trades within traderTime span
		 *  - Keep optimizing its config by optimizing traderTime-bots within optimizerWindow history
		 *  	- E.g. Give me the most successful daytrader configuration for the last three days of data, then use that
		 *  
		 *  Needs:
		 *  
		 *  - Refactor MacdBotTrainer so you can create an instance with a specific configuration
		 *  - Needs a way to do an non-realtime parse of ticker history collected
		 *  - New, internally created bots should keep using the same wallet/tradefloor
		 *  - Needs a wakeUp delay parameter to avoid early measurement error, just like MacdBot
		 */		
	}
	
	@Override
	public long getLastTick() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void tick(long tick) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public long getTimestep() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public ITradeFloor getTradeFloor() {
		// TODO Auto-generated method stub
		return null;
	}
	
	
}
