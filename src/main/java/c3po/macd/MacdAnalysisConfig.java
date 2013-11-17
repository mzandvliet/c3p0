package c3po.macd;

/**
 * Configuration struct for the MacdNode.
 * Makes it easier to separate the
 * configuration parameters from the logic.
 * 
 */
public class MacdAnalysisConfig {
	public final int slowPeriod;
	public final int fastPeriod;
	public final int signalPeriod;
	
	public MacdAnalysisConfig(int slowPeriod, int fastPeriod, int signalPeriod) {
		this.slowPeriod = slowPeriod;
		this.fastPeriod = fastPeriod;
		this.signalPeriod = signalPeriod;
	}
	
	public String toString() {
		return String.format("[MacdNodeConfig - slow: %d, fast: %d, signal: %d]", slowPeriod, fastPeriod, signalPeriod);
	}
}
