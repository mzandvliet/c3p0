package c3po.simulation;

import static org.junit.Assert.*;

import java.util.LinkedList;

import org.junit.Test;

import c3po.Sample;
import c3po.macd.MacdTraderNode;

public class OptimalTradeAnalysisTest {
	@Test
	public void testCalculateTradeAdvice() {
		
		LinkedList<Sample> data = new LinkedList<Sample>();
		data.add(new Sample(1, 1));
		
		// Small dip should give negative
		data.add(new Sample(2, 0.995));
		assertEquals(-0.5, OptimalTradeAdviceAnalysis.calculateTradeAdvice(data), 0.001d);
		
		// Raise should give positive
		data.add(new Sample(3, 1.007));
		assertEquals(0.7, OptimalTradeAdviceAnalysis.calculateTradeAdvice(data), 0.001d);
		
		// Min advice is -1
		data.add(new Sample(4, 0.895));
		assertEquals(-1, OptimalTradeAdviceAnalysis.calculateTradeAdvice(data), 0.001d);
		
		// Max advice is 1
		data.add(new Sample(5, 1.027));
		assertEquals(1, OptimalTradeAdviceAnalysis.calculateTradeAdvice(data), 0.001d);

	}
}
