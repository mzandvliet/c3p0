package c3po.utils;

import java.util.ArrayList;
import java.util.List;

import c3po.Sample;

public class SignalMath {
	public static long lerp(long low, long high, double lerp) {
		return low + (long)((low-high) * lerp);
	}
	
	public static Sample lerp(Sample oldest, Sample newest, long timestamp) {
		long timeDelta = newest.timestamp - oldest.timestamp;
		
		if (timeDelta == 0)
			throw new IllegalArgumentException("oldest and newest samples have same timestamp");
		
		long indexTimeDelta = timestamp - oldest.timestamp;
		double lerp = (double)indexTimeDelta / (double)timeDelta;
		double valueDelta = newest.value - oldest.value;
		double value = oldest.value + valueDelta * lerp;
		return new Sample(timestamp, value);
	}
	
	public static Sample basicMovingAverage(final List<Sample> kernelBuffer) {
		int size = kernelBuffer.size();
		
		double total = 0;
		for (int i = 0; i < size; i++) {
			total += kernelBuffer.get(i).value;
		}
		
		return new Sample(kernelBuffer.get(0).timestamp, total / (double) size);
	}

	public static List<Sample> filterExpMovingAverage(List<Sample> signals, int kernelSize) {
		int size = signals.size();
		List<Sample> smoothSignals = new ArrayList<Sample>(size);
		  
		for (int i = 0; i < size; i++) {
			smoothSignals.add(filterExpMovingAverage(signals.get(Math.max(i-1,0)), signals.get(i), kernelSize));
		}
		
		return smoothSignals;
	}
	
	public static Sample filterExpMovingAverage(final Sample lastSample, final Sample newest, final int kernelSize) {		
		if (lastSample == Sample.none)
			return Sample.copy(newest);
		
		double alpha = 2.0 / ((double)kernelSize + 1.0);
		double previous = lastSample.value;
		double current = newest.value * alpha + previous * (1.0 - alpha);

		return new Sample(newest.timestamp, current);
	}
	
	public static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}
	
	public static double clamp(double value, double min, double max) {
		return Math.max(min, Math.min(max, value));
	}
	
	public static double getRandomDouble(double min, double max) {
		return min + (Math.random() * (max-min));
	}
	
	public static long getRandomLong(long min, long max) {
		return min + (long)(Math.random() * (double)(max-min));
	}
}
