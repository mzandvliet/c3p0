package c3po;

import java.util.ArrayList;
import java.math.*;

public class Indicators {
	public static Signal lerp(Signal oldest, Signal newest, long timestamp) {
		long timeDelta = newest.timestamp - oldest.timestamp;
		long localTime = newest.timestamp - timestamp;
		double lerp = (double)localTime / (double)timeDelta;
		double valueDelta = newest.value - oldest.value;
		return new Signal(timestamp, valueDelta * lerp);
	}
	
	public static double[] filterMovingAverage(double[] values, double kernelSize) {
		double[] result = new double[values.length];
		
		for (int i = 0; i < values.length; i++) {
			double kernelSum = 0;
		    for (int j = 0; j < kernelSize; j++) {
		      int kernelIndex = clamp(i + j, 0, values.length-1);
		      kernelSum += values[kernelIndex];
		    }
		    result[i] = kernelSum / kernelSize;
		  }
		
		return result;
	}
	
	public static double[] filterExpMovingAverage(double[] values, int kernelSize) {
		double alpha = 2.0 / ((double)kernelSize + 1.0);
		double[] averageValues = new double[values.length];
		  
		double previous = values[values.length-1];
		averageValues[values.length-1] = previous;
		double current;
		for (int i = values.length - 2; i >= 0; i--) {
			current = values[i] * alpha + previous * (1.0 - alpha);
			previous = current;
			averageValues[i] = current;
		}
		
		return averageValues;
	}
	
	public static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}
	
	public static double[] combine(double[] a, double[] b, ICombiner combiner) {
		double[] result = new double[a.length];
		
		for (int i = 0; i < a.length; i++) {
			result[i] = combiner.combine(a[i], b[i]);
		}
		
		return result;
	}
	
	public interface ICombiner {
		public double combine(double a, double b);
	}
}
