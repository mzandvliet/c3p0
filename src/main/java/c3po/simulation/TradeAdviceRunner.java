package c3po.simulation;

import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.util.Date;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import c3po.bitstamp.BitstampSimulationOrderBookDbSource;
import c3po.bitstamp.BitstampSimulationTickerDbSource;
import c3po.clock.ISimulationClock;
import c3po.clock.SimulationClock;
import c3po.node.GraphingNode;
import c3po.utils.Time;
import c3po.DbConnection;

public class TradeAdviceRunner {
	//================================================================================
    // Static Properties
    //================================================================================
	
	private static final Logger LOGGER = LoggerFactory.getLogger(TradeAdviceRunner.class);
	
	// Earliest time 1384079023000l
	private final static long simulationEndTime = new Date().getTime() - Time.DAYS * 1;
	private final static long simulationStartTime = simulationEndTime - Time.HOURS * 1;

	
	private final static long interpolationTime = 60 * Time.SECONDS;
	private final static long timestep = 60 * Time.SECONDS;
	
	
	private final static long graphInterval = 60 * Time.SECONDS;
	
	//================================================================================
    // Main
    //================================================================================
	
	public static void main(String[] args) throws ClassNotFoundException, SQLException {
		// Set up global signal tree
		
		DbConnection dbConnection = new DbConnection(new InetSocketAddress("c3po.ramjetanvil.com", 3306), "c3po", "D7xpJwzGJEWf5qWB");
		dbConnection.open();
		
		final BitstampSimulationTickerDbSource tickerNode = new BitstampSimulationTickerDbSource(
				timestep,
				interpolationTime,
				dbConnection,
				simulationStartTime,
				simulationEndTime
				);
		
		// Create bot
		OptimalTradeAdviceAnalysis bot = new OptimalTradeAdviceAnalysis(timestep, tickerNode.getOutputLast(), Time.MINUTES * 10);
		
		// Create the grapher
		GraphingNode grapher = new GraphingNode(graphInterval, "Ticker",
				tickerNode.getOutputLast()
		);
		
		GraphingNode diffGrapher = new GraphingNode(graphInterval, "TradeAdvice",
				bot.getOutputTradeAdvice());
		
		// Create a clock
		ISimulationClock botClock = new SimulationClock(timestep, interpolationTime);
		botClock.addListener(bot);
		botClock.addListener(grapher);
		botClock.addListener(diffGrapher);
		
		// Run the program

		botClock.run(simulationStartTime, simulationEndTime);
		
		dbConnection.close();
		
		
		// Log results
		
		grapher.pack();
		grapher.setVisible(true);
		
		diffGrapher.pack();
		diffGrapher.setVisible(true);		
	}
}
