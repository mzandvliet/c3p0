package c3po.utils;

import static org.junit.Assert.*;

import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import c3po.Sample;
import c3po.utils.SignalMath;

public class SignalMathTest {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testBasicMovingAverageListOfSampleInt() {
		List<Sample> signals = new LinkedList<Sample>();
		signals.add(new Sample(1l, 1.0d));
		signals.add(new Sample(2l, 2.0d));
		signals.add(new Sample(3l, 3.0d));
		signals.add(new Sample(4l, 4.0d));
		signals.add(new Sample(5l, 5.0d));
		signals.add(new Sample(6l, 6.0d));
		signals.add(new Sample(7l, 7.0d));
		signals.add(new Sample(8l, 8.0d));
		signals.add(new Sample(9l, 9.0d));
		signals.add(new Sample(10l, 10.0d));
		
		Sample movingAvg = SignalMath.basicMovingAverage(signals);
		
		assertEquals(5, movingAvg.value, 0.01);
		
	}

	@Test
	public void testFilterMovingAverageListOfSampleSampleInt() {
		fail("Not yet implemented");
	}

}
