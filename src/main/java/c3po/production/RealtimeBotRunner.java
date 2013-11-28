package c3po.production;

import c3po.*;

import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import c3po.bitstamp.BitstampSimulationTradeFloor;
import c3po.bitstamp.BitstampTickerSource;
import c3po.bitstamp.BitstampTickerDbSource;
import c3po.bitstamp.BitstampTradeFloor;
import c3po.IClock;
import c3po.ITradeFloor;
import c3po.Time;
import c3po.macd.*;

public class RealtimeBotRunner {

	private static final Logger LOGGER = LoggerFactory.getLogger(RealtimeBotRunner.class);
	private final static String jsonUrl = "http://www.bitstamp.net/api/ticker/";

	private final static long interpolationTime = 2 * Time.MINUTES;
	private final static long timestep = 1 * Time.MINUTES;
	
	private static final double walletDollarStart = 0.0;
	private static final double walletBtcStart = 0.0;

	public static void main(String[] args) throws ClassNotFoundException, SQLException {
			
		/**
		 * Bot Config
		 */
		MacdAnalysisConfig analysisConfig = new MacdAnalysisConfig(
				218 * Time.MINUTES,
				325 * Time.MINUTES,
				311 * Time.MINUTES);
		
		MacdTraderConfig traderConfig = new MacdTraderConfig(
				1.6356,
				-4.6798);
		
		MacdBotConfig config = new MacdBotConfig(timestep, analysisConfig, traderConfig);
		
		// Set up global signal tree
		final BitstampTickerSource tickerNode = new BitstampTickerDbSource(timestep, interpolationTime, new InetSocketAddress("94.208.87.249", 3309), "c3po", "D7xpJwzGJEWf5qWB");

		/**
		 * Tradefloor + Wallet Initialization
		 */
		final IWallet wallet = new Wallet(walletDollarStart, walletBtcStart);
		
		final ITradeFloor tradeFloor =  new BitstampTradeFloor(
				tickerNode.getOutputLast(),
				tickerNode.getOutputBid(),
				tickerNode.getOutputAsk()
		);
		
		// Update the wallet with the real values
		tradeFloor.updateWallet(wallet);
		
		final DebugTradeLogger tradeLogger = new DebugTradeLogger();
		tradeFloor.addTradeListener(tradeLogger);
		
		// Create bot
		IBot bot = new MacdBot(config, tickerNode.getOutputLast(), wallet, tradeFloor);
		
		DbTradeLogger dbTradeLogger = new DbTradeLogger(bot, new InetSocketAddress("94.208.87.249", 3309), "c3po", "D7xpJwzGJEWf5qWB");
		dbTradeLogger.open();
		dbTradeLogger.startSession(new Date().getTime(), wallet.getWalletUsd(), wallet.getWalletBtc());

		
		// Create a clock
		IClock botClock = new RealtimeClock(timestep, Math.max(analysisConfig.slowPeriod, analysisConfig.signalPeriod), interpolationTime);
		botClock.addListener(bot);
		
		// Run the program
		tickerNode.open();
		botClock.run();
		tickerNode.close();
		
		// Log results
		LOGGER.debug("Num trades: " + tradeLogger.getActions().size() + ", Profit: " + (tradeFloor.getWalletValueInUsd(wallet) - walletDollarStart));
	}
}
