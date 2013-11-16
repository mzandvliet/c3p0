package c3po;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import c3po.macd.MacdNodeConfig;

/* 
 * MacdSource
 * 
 * This is kind of a macro, a signal tree wrapped in a single node for easy use, with
 * defined inputs and outputs for hooking it into other signal trees.
 * 
 * Todo:
 * - Expose node tree for manual introspection (like chart drawing of buffers or visualizing structure)
 */
public class MacdNode implements INode {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(MacdNode.class);
	
	private final int numSignals = 5;
	private ISignal[] signals;
	private long lastTick = -1;
	
	INode fastNode;
	INode slowNode;
	INode macdNode;
	INode signalNode;
	INode diffNode;
	
	private final MacdNodeConfig config;
	
	public MacdNode(ISignal input, MacdNodeConfig config) {
		this.config = config;
		this.signals = new OutputSignal[numSignals];
		
		// Create internal signal tree, hook all intermediate results up to outputs
		fastNode = new ExpMovingAverageNode(input, config.getFastSampleCount());
		slowNode = new ExpMovingAverageNode(input, config.getSignalSampleCount());
		macdNode = new SubtractNode(fastNode.getOutput(0), slowNode.getOutput(0));
		signalNode = new ExpMovingAverageNode(macdNode.getOutput(0), config.getSignalSampleCount());
		diffNode = new SubtractNode(macdNode.getOutput(0), signalNode.getOutput(0));
		
		this.signals[SignalNames.FAST.ordinal()] = fastNode.getOutput(0);
		this.signals[SignalNames.SLOW.ordinal()] = slowNode.getOutput(0);
		this.signals[SignalNames.MACD.ordinal()] = macdNode.getOutput(0);
		this.signals[SignalNames.SIGNAL.ordinal()] = signalNode.getOutput(0);
		this.signals[SignalNames.DIFFERENCE.ordinal()] = diffNode.getOutput(0);
		
		LOGGER.debug(String.format("Initiated MacdNode with " + config));
	}
	
		
	public MacdNodeConfig getConfig() {
		return config;
	}


	@Override
	public int getNumOutputs() {
		return numSignals;
	}
	
	@Override
	public ISignal getOutput(int i) {
		return signals[i];
	}
	
	public void tick(long tick) {
		if (tick >= lastTick) {
			diffNode.tick(tick);
			lastTick = tick;
		}
	}
	
	public enum SignalNames {
		FAST,
		SLOW,
		MACD,
		SIGNAL,
		DIFFERENCE
	}
}
