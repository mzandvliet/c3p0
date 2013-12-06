package c3po.macd;

import c3po.*;

import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.util.Date;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import c3po.bitstamp.BitstampSimulationTradeFloor;
import c3po.bitstamp.BitstampSimulationTickerDbSource;
import c3po.DbConnection;
import c3po.IClock;
import c3po.ISignal;
import c3po.ITradeFloor;
import c3po.ITradeListener;
import c3po.IWallet;
import c3po.Time;

public class MacdBot extends AbstractTickable implements IBot<MacdBotConfig> {
	//================================================================================
    // Static Properties
    //================================================================================
	
	private static final Logger LOGGER = LoggerFactory.getLogger(MacdBot.class);
	
	// Earliest time 1384079023000l
	private final static long simulationStartTime = 1384079023000l;
	private final static long simulationEndTime = new Date().getTime();
	
	private final static long interpolationTime = 2 * Time.MINUTES;
	private final static long timestep = 1 * Time.MINUTES;
	
	private final static double walletStartUsd = 1000.0d;
	private final static double walletStartBtcInUsd = 0.0d;
	
	private final static long graphInterval = 10 * Time.MINUTES;
	
	//================================================================================
    // Main
    //================================================================================
	
	public static void main(String[] args) throws ClassNotFoundException, SQLException {
		// Set up global signal tree
		
		final BitstampSimulationTickerDbSource tickerNode = new BitstampSimulationTickerDbSource(
				timestep,
				interpolationTime,
				new InetSocketAddress("94.208.87.249", 3309),
				"c3po",
				"D7xpJwzGJEWf5qWB",
				simulationStartTime,
				simulationEndTime
				);
		tickerNode.open();
		
		final ITradeFloor tradeFloor =  new BitstampSimulationTradeFloor(
				tickerNode.getOutputLast(),
				tickerNode.getOutputBid(),
				tickerNode.getOutputAsk()
		);
		
		double walletStartBtc = walletStartBtcInUsd / tickerNode.getOutputLast().getSample(simulationStartTime).value;
		final IWallet wallet = new Wallet(walletStartUsd, walletStartBtc);
		
		// Create bot config
		MacdAnalysisConfig analysisConfig = new MacdAnalysisConfig(
				39 * Time.MINUTES,
				267 * Time.MINUTES,
				235 * Time.MINUTES);
		
		MacdTraderConfig traderConfig = new MacdTraderConfig(
				4.8876,
				-9.2317);
		MacdBotConfig config = new MacdBotConfig(timestep, analysisConfig, traderConfig);
		
		DbConnection dbConnection = new DbConnection(new InetSocketAddress("94.208.87.249", 3309), "c3po", "D7xpJwzGJEWf5qWB");
		dbConnection.open();
		
		// Create bot
		
		int botId = Math.abs(new Random().nextInt());
		MacdBot bot = new MacdBot(botId, config, tickerNode.getOutputLast(), wallet, tradeFloor);
		
		// Create loggers
		
		DebugTradeLogger tradeLogger = new DebugTradeLogger();
		bot.addTradeListener(tradeLogger);
		
		DbTradeLogger dbLogger = new DbTradeLogger(bot, dbConnection);
		dbLogger.startSession(simulationStartTime);
		
//		EmailTradeLogger mailLogger = new EmailTradeLogger("martijn@ramjetanvil.com", "jopast@gmail.com");
//		bot.addTradeListener(mailLogger);
		
		// Create the grapher
		
		GraphingNode grapher = new GraphingNode(graphInterval, "Ticker", 
				tickerNode.getOutputLast(),
				tickerNode.getOutputBid(),
				tickerNode.getOutputAsk()
				);
		bot.addTradeListener(grapher);
		
		GraphingNode diffGrapher = new GraphingNode(graphInterval, "Macd", 
				bot.analysisNode.getOutputDifference()
				);
		bot.addTradeListener(diffGrapher);
		
		// Create a clock
		
		IClock botClock = new SimulationClock(timestep, simulationStartTime, simulationEndTime, interpolationTime);
		botClock.addListener(bot);
		botClock.addListener(grapher);
		botClock.addListener(diffGrapher);
		
		// Run the program

		botClock.run();
		
		tickerNode.close();
		
		
		// Log results
		
		grapher.pack();
		grapher.setVisible(true); // Show graph *after* simulation because otherwise annotation adding causes exceptions
		
		diffGrapher.pack();
		diffGrapher.setVisible(true); // Show graph *after* simulation because otherwise annotation adding causes exceptions
		
		tradeLogger.writeLog();
		LOGGER.debug("Num trades: " + tradeLogger.getActions().size() + ", Wallet: " + tradeFloor.getWalletValueInUsd(wallet));

		dbConnection.close();
	}
	
	
	//================================================================================
    // Properties
    //================================================================================
	
	private final int id;
	private final MacdBotConfig config;
	private final IWallet wallet;
	private final ITradeFloor tradeFloor;
	
    // Debug references
	
	private final MacdAnalysisNode analysisNode;
	private final MacdTraderNode traderNode;
	

	//================================================================================
    // Methods
    //================================================================================
	
	public MacdBot(int id, MacdBotConfig config, ISignal ticker, IWallet wallet, ITradeFloor tradeFloor) {
		super(config.timestep);
		this.id = id;
		this.config = config;
		this.wallet = wallet;
		this.tradeFloor = tradeFloor;
		
		// Define the signal tree		
		
		analysisNode = new MacdAnalysisNode(timestep, ticker, config.analysisConfig);
		long startDelayInTicks = config.analysisConfig.max() / timestep;
		traderNode = new MacdTraderNode(timestep, analysisNode.getOutputDifference(), wallet, tradeFloor, config.traderConfig, startDelayInTicks);
	}

	@Override
	public void onNewTick(long tick) {
		tradeFloor.updateWallet(wallet);		
		traderNode.tick(tick);
	}
	
	@Override
	public long getTimestep() {
		return config.timestep;
	}

	@Override
	public IWallet getWallet() {
		return wallet;
	}

	@Override
	public ITradeFloor getTradeFloor() {
		return tradeFloor;
	}
	
	@Override
	public int getId() {
		return id;
	}

	public MacdAnalysisNode getAnalysisNode() {
		return analysisNode;
	}
	
	public MacdTraderNode getTraderNode() {
		return traderNode;
	}
	
	public MacdBotConfig getConfig() {
		return config;
	}

	public String toString() {
		return String.format("%s, %s", analysisNode.getConfig(), traderNode.getConfig());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((config == null) ? 0 : config.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MacdBot other = (MacdBot) obj;
		if (config == null) {
			if (other.config != null)
				return false;
		} else if (!config.equals(other.config))
			return false;
		return true;
	}

	@Override
	public void addTradeListener(ITradeListener listener) {
		traderNode.addTradeListener(listener);
	}

	@Override
	public void removeListener(ITradeListener listener) {
		traderNode.removeListener(listener);
	}
}
