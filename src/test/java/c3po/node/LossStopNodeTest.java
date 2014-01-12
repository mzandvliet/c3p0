package c3po.node;

import static org.junit.Assert.*;

import org.junit.Test;

public class LossStopNodeTest {

	@Test
	public void testCalculateTradeAdvice() {
		assertEquals(-0.5d, LossStopNode.calculateTradeAdvice(100.0, 90.0, 5, 15), 0.00001d);
		assertEquals(0d, LossStopNode.calculateTradeAdvice(100.0, 96.0, 5, 15), 0.00001d);
		assertEquals(-1d, LossStopNode.calculateTradeAdvice(100.0, 80.0, 5, 15), 0.00001d);
	}
}
