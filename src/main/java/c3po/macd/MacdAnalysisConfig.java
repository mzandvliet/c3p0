package c3po.macd;

/**
 * Configuration struct for the MacdNode.
 * Makes it easier to separate the
 * configuration parameters from the logic.
 * 
 */
public class MacdAnalysisConfig {
	public final int fastPeriod;
	public final int slowPeriod;
	public final int signalPeriod;
	
	public MacdAnalysisConfig(int fastPeriod, int slowPeriod, int signalPeriod) {
		this.slowPeriod = slowPeriod;
		this.fastPeriod = fastPeriod;
		this.signalPeriod = signalPeriod;
	}
	
	public int max() {
		return Math.max(fastPeriod, Math.max(signalPeriod, slowPeriod));
	}
	
	public String toString() {
		return String.format("[AnalysisConfig - fast: %d, slow: %d, signal: %d]", fastPeriod, slowPeriod, signalPeriod);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + fastPeriod;
		result = prime * result + signalPeriod;
		result = prime * result + slowPeriod;
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
