package c3po.macd;

import c3po.Time;

/**
 * Configuration struct for the MacdNode.
 * Makes it easier to separate the
 * configuration parameters from the logic.
 * 
 */
public class MacdAnalysisConfig {
	public final long fastPeriod;
	public final long slowPeriod;
	public final long signalPeriod;
	
	public MacdAnalysisConfig(long fast, long slow, long signal) {
		this.fastPeriod = fast;
		this.slowPeriod = slow;
		this.signalPeriod = signal;
	}
	
	public long max() {
		return Math.max(fastPeriod, Math.max(signalPeriod, slowPeriod));
	}
	
	public String toString() {
		return String.format("[AnalysisConfig - fast: %d min, slow: %d min, signal: %d min]", fastPeriod / Time.MINUTES, slowPeriod / Time.MINUTES, signalPeriod / Time.MINUTES);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (fastPeriod ^ (fastPeriod >>> 32));
		result = prime * result + (int) (signalPeriod ^ (signalPeriod >>> 32));
		result = prime * result + (int) (slowPeriod ^ (slowPeriod >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MacdAnalysisConfig other = (MacdAnalysisConfig) obj;
		if (fastPeriod != other.fastPeriod)
			return false;
		if (signalPeriod != other.signalPeriod)
			return false;
		if (slowPeriod != other.slowPeriod)
			return false;
		return true;
	}
}
