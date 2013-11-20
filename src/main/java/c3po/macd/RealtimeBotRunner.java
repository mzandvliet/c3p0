package c3po.macd;

import c3po.*;

import java.net.InetSocketAddress;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import c3po.BitstampTickerCsvSource;
import c3po.BitstampSimulationTradeFloor;
import c3po.IClock;
import c3po.ITradeFloor;
import c3po.RealtimeClock;

public class RealtimeBotRunner {

	private static final Logger LOGGER = LoggerFactory.getLogger(RealtimeBotRunner.class);
	private final static String jsonUrl = "http://www.bitstamp.net/api/ticker/";

	private final static long clockTimestep = 1000;
	private final static long botTimestep = 1000;
	
	private static final double walletDollarStart = 1000.0;
	private static final double walletBtcStart = 0.0;

	public static void main(String[] args) throws ClassNotFoundException, SQLException {
		// Set up global signal tree
		
		final BitstampTickerSource tickerNode = new BitstampTickerDbSource(new InetSocketAddress("94.208.87.249", 3309), "c3po", "D7xpJwzGJEWf5qWB");
		
		final IWallet wallet = new Wallet(walletDollarStart, walletBtcStart);
		
		final ITradeFloor tradeFloor =  new BitstampSimulationTradeFloor(
				tickerNode.getOutputLast(),
				tickerNode.getOutputBid(),
				tickerNode.getOutputAsk()
		);
		
		final DebugTradeLogger tradeLogger = new DebugTradeLogger();
		tradeFloor.addListener(tradeLogger);
		
		// Create bot config
		
		// todo: this is still in number-of-ticks, which means it depends on bot's timeStep, should change to units of time
		
		MacdAnalysisConfig analysisConfig = new MacdAnalysisConfig(640,780,985); // Todo: trader.startDelay is proportional to this, maybe Max(fast,slow,signal)
		MacdTraderConfig traderConfig = new MacdTraderConfig(0.0267, 0.4547, 0.2184, 0.0037, 2440000, 4042000);
		MacdBotConfig config = new MacdBotConfig(botTimestep, analysisConfig, traderConfig);
		
		// Create bot
		
		IBot bot = new MacdBot(config, tickerNode.getOutputLast(), wallet, tradeFloor);
		
		// Create a clock
		
		IClock botClock = new RealtimeClock(clockTimestep, 200000l);
		botClock.addListener(bot);
		
		// Run the program
		
		tickerNode.open();
		
		botClock.run();
		
		tickerNode.close();
		
		// Log results
		
		LOGGER.debug("Num trades: " + tradeLogger.getActions().size() + ", Profit: " + (tradeFloor.getWalletValueInUsd(wallet) - walletDollarStart));
	}
}
