package c3po;

import static org.junit.Assert.*;

import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.util.Date;

import org.junit.Test;

import c3po.bitstamp.BitstampTickerDbSource;

public class BitstampTickerDbSourceTest {

	private final static long timestep = 60000;
	private final static long interpolationTime = 120000;
	
	@Test
	public void testRealtime() throws ClassNotFoundException, SQLException, InterruptedException {
		BitstampTickerDbSource tickerNode = new BitstampTickerDbSource(timestep, interpolationTime, new InetSocketAddress("94.208.87.249", 3309), "c3po", "D7xpJwzGJEWf5qWB");
		
		
		tickerNode.open();
		
		for(int i = 0; i < 10; i++) {
			
			tickerNode.tick(new Date().getTime() / 1000);
			
			Thread.sleep(1000);
		}
		
		tickerNode.close();
	}

}
