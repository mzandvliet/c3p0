package c3po;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class MovingAverageNodeTest {

	@Test
	public void test() {
		// Input data
		final List<Sample> inputData = new ArrayList<Sample>();
		inputData.add(new Sample(1000l, 1));
		inputData.add(new Sample(2000l, 2));
		inputData.add(new Sample(3000l, 3));
		inputData.add(new Sample(4000l, 4));
		inputData.add(new Sample(5000l, 5));

		// Window of 2000ms
		MovingAverageNode movingAverageNode = new MovingAverageNode(1000l, 2000l, new ISignal() {
			@Override
			public Sample peek() {
				return null;
			}
			
			@Override
			public Sample getSample(long tick) {
				return inputData.get((int) ((tick / 1000) - 1));
			}
		});
		
		// Expecting the average of the past 2 samples
		movingAverageNode.tick(1000l);
		movingAverageNode.tick(2000l);
		assertEquals(1.5, movingAverageNode.getOutput(0).getSample(2000l).value, 0.01);
		movingAverageNode.tick(3000l);
		assertEquals(2.5, movingAverageNode.getOutput(0).getSample(3000l).value, 0.01);
		movingAverageNode.tick(4000l);
		assertEquals(3.5, movingAverageNode.getOutput(0).getSample(4000l).value, 0.01);
		movingAverageNode.tick(5000l);
		assertEquals(4.5, movingAverageNode.getOutput(0).getSample(5000l).value, 0.01);
	}

}
