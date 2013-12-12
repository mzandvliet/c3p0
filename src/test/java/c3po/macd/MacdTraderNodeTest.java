package c3po.macd;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class MacdTraderNodeTest {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testCalculateCurrentSellThreshold() {
		assertEquals(0, MacdTraderNode.calculateCurrentSellThreshold(-6, 100, 300, 20), 0.001d);
		assertEquals(-4.8, MacdTraderNode.calculateCurrentSellThreshold(-6, 100, 101, 20), 0.001d);
		assertEquals(-5.4, MacdTraderNode.calculateCurrentSellThreshold(-6, 100, 101, 10), 0.001d);
		assertEquals(-6, MacdTraderNode.calculateCurrentSellThreshold(-6, 0, 101, 10), 0.001d);
		assertEquals(-6, MacdTraderNode.calculateCurrentSellThreshold(-6, 100, 90, 10), 0.001d);
		assertEquals(0, MacdTraderNode.calculateCurrentSellThreshold(-6, 100, 120, 200), 0.001d);
		assertEquals(-6, MacdTraderNode.calculateCurrentSellThreshold(-6, 100, 120, -10), 0.001d);
	}
}
