package c3po;

import java.util.ArrayList;
import java.util.List;

public class SignalMath {
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
	
	public static Sample basicMovingAverage(final List<Sample> signals, final int kernelSize) {
		int size = signals.size();	

		int sampleCount = Math.min(size, kernelSize);
		
		double total = 0;
		for (int i = 0; i < sampleCount; i++) {
			total += signals.get(i).value;
		}
		
		return new Sample(signals.get(0).timestamp, total / (double) kernelSize);
	}
	
	public static List<Sample> filterMovingAverage(final List<Sample> signals, final int kernelSize) {
		int size = signals.size();
		List<Sample> smoothSignals = new ArrayList<Sample>(size);
		
		for (int i = 0; i < size; i++) {
			smoothSignals.add(filterMovingAverage(smoothSignals, signals.get(i), kernelSize));
		}
		
		return smoothSignals;
	}
	
	public static Sample filterMovingAverage(final List<Sample> smoothSignals, final Sample newest, final int kernelSize) {
		int size = smoothSignals.size();
		
		if (size == 0)
			return Sample.copy(newest);
		
		double kernelSum = 0;
		
		// From oldest to newest in kernel, clamping in case we don't have enough samples
		for (int j = 0; j < kernelSize-1; j++) {
			int kernelIndex = clamp(size - j, 0, size-1);
			Sample current = smoothSignals.get(kernelIndex);
			kernelSum += current.value;
		}
		kernelSum += newest.value;

		return new Sample(newest.timestamp, kernelSum / kernelSize);
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
