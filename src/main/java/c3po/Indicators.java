package c3po;

import java.util.ArrayList;
import java.util.List;
import java.math.*;

public class Indicators {
	public static Signal lerp(Signal oldest, Signal newest, long timestamp) {
		long timeDelta = newest.timestamp - oldest.timestamp;
		long localTime = newest.timestamp - timestamp;
		double lerp = (double)localTime / (double)timeDelta;
		double valueDelta = newest.value - oldest.value;
		return new Signal(timestamp, valueDelta * lerp);
	}
	
	public static List<Signal> filterMovingAverage(final List<Signal> signals, final int kernelSize) {
		int size = signals.size();
		List<Signal> smoothSignals = new ArrayList<Signal>(size);
		
		for (int i = 0; i < size; i++) {
			smoothSignals.add(filterMovingAverage(smoothSignals, signals.get(i), kernelSize));
		}
		
		return smoothSignals;
	}
	
	public static Signal filterMovingAverage(final List<Signal> smoothSignals, final Signal newest, final int kernelSize) {
		int size = smoothSignals.size();
		
		if (size == 0)
			return Signal.copy(newest);
		
		double kernelSum = 0;
		
		// From oldest to newest in kernel, clamping in case we don't have enough samples
		for (int j = 0; j < kernelSize-1; j++) {
			int kernelIndex = clamp(size - j, 0, size-1);
			Signal current = smoothSignals.get(kernelIndex);
			kernelSum += current.value;
		}
		kernelSum += newest.value;

		return new Signal(newest.timestamp, kernelSum / kernelSize);
	}
	
	// Todo: make recursive filtering methods with signature (Signal previous, Signal current)
	
//	public static double[] filterExpMovingAverage(double[] values, int kernelSize) {
//		double alpha = 2.0 / ((double)kernelSize + 1.0);
//		double[] averageValues = new double[values.length];
//		  
//		double previous = values[values.length-1];
//		averageValues[values.length-1] = previous;
//		double current;
//		for (int i = values.length - 2; i >= 0; i--) {
//			current = values[i] * alpha + previous * (1.0 - alpha);
//			previous = current;
//			averageValues[i] = current;
//		}
//		
//		return averageValues;
//	}
	
	public static List<Signal> filterExpMovingAverage(List<Signal> signals, int kernelSize) {
		int size = signals.size();
		List<Signal> smoothSignals = new ArrayList<Signal>(size);
		  
		for (int i = 0; i < size; i++) {
			smoothSignals.add(filterExpMovingAverage(smoothSignals, signals.get(i), kernelSize));
		}
		
		return smoothSignals;
	}
	
	public static Signal filterExpMovingAverage(final List<Signal> smoothSignals, final Signal newest, final int kernelSize) {
		int size = smoothSignals.size();
		
		if (size == 0)
			return Signal.copy(newest);
		
		double alpha = 2.0 / ((double)kernelSize + 1.0);
		double previous = smoothSignals.get(size-1).value;
		double current = newest.value * alpha + previous * (1.0 - alpha);

		return new Signal(newest.timestamp, current);
	}
	
	public static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}
	
//	public static double[] combine(double[] a, double[] b, ICombiner combiner) {
//		double[] result = new double[a.length];
//		
//		for (int i = 0; i < a.length; i++) {
//			result[i] = combiner.combine(a[i], b[i]);
//		}
//		
//		return result;
//	}
//	
//	public interface ICombiner {
//		public double combine(double a, double b);
//	}
}
