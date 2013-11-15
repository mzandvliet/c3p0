package c3po.macd;

/**
 * Configuration struct for the MacdNode.
 * Makes it easier to separate the
 * configuration parameters from the logic.
 * 
 */
public class MacdNodeConfig {
	private final int slowSampleCount;
	private final int fastSampleCount;
	private final int signalSampleCount;
	
	public MacdNodeConfig(int slowSampleCount, int fastSampleCount, int signalSampleCount) {
		this.slowSampleCount = slowSampleCount;
		this.fastSampleCount = fastSampleCount;
		this.signalSampleCount = signalSampleCount;
	}
	
	public int getSlowSampleCount() {
		return slowSampleCount;
	}
	
	public int getFastSampleCount() {
		return fastSampleCount;
	}
	
	public int getSignalSampleCount() {
		return signalSampleCount;
	}
	
	public String toString() {
		return String.format("[MacdNodeConfig - slow: %d, fast: %d, signal: %d]", slowSampleCount, fastSampleCount, slowSampleCount);
	}
}
