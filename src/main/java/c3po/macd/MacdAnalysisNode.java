package c3po.macd;

import c3po.*;
import c3po.node.ExpMovingAverageNode;
import c3po.node.INode;
import c3po.node.SubtractNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* 
 * MacdSource
 * 
 * This is kind of a macro, a signal tree wrapped in a single node for easy use, with
 * defined inputs and outputs for hooking it into other signal trees.
 * 
 * Todo:
 * - Expose node tree for manual introspection (like chart drawing of buffers or visualizing structure)
 */
public class MacdAnalysisNode extends AbstractTickable implements INode {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(MacdAnalysisNode.class);
	
	private final ISignal[] signals;
	
	private final INode fastNode;
	private final INode slowNode;
	private final INode macdNode;
	private final INode signalNode;
	private final INode diffNode;
	
	private final MacdAnalysisConfig config;
	
	public MacdAnalysisNode(long timestep, ISignal price, ISignal volume, MacdAnalysisConfig config) {
		super(timestep);
		this.config = config;
		this.signals = new OutputSignal[6];
		
		// Create internal signal tree, hook all intermediate results up to outputs
		fastNode = new ExpMovingAverageNode(timestep, config.fastPeriod, price);
		slowNode = new ExpMovingAverageNode(timestep, config.slowPeriod, price);
		macdNode = new SubtractNode(timestep, fastNode.getOutput(0), slowNode.getOutput(0));
		signalNode = new ExpMovingAverageNode(timestep, config.signalPeriod, macdNode.getOutput(0));
		diffNode = new SubtractNode(timestep, macdNode.getOutput(0), signalNode.getOutput(0));
		
		this.signals[SignalNames.FAST.ordinal()] = fastNode.getOutput(0);
		this.signals[SignalNames.SLOW.ordinal()] = slowNode.getOutput(0);
		this.signals[SignalNames.MACD.ordinal()] = macdNode.getOutput(0);
		this.signals[SignalNames.SIGNAL.ordinal()] = signalNode.getOutput(0);
		this.signals[SignalNames.DIFFERENCE.ordinal()] = diffNode.getOutput(0);
	}
	
	public ISignal getOutputFast() {
		return this.signals[SignalNames.FAST.ordinal()];
	}
	
	public ISignal getOutputSlow() {
		return this.signals[SignalNames.SLOW.ordinal()];
	}
	
	public ISignal getOutputMacd() {
		return this.signals[SignalNames.MACD.ordinal()];
	}
	
	public ISignal getOutputSignal() {
		return this.signals[SignalNames.SIGNAL.ordinal()];
	}
	
	public ISignal getOutputDifference() {
		return this.signals[SignalNames.DIFFERENCE.ordinal()];
	}

	public MacdAnalysisConfig getConfig() {
		return config;
	}
	
	@Override
	public int getNumOutputs() {
		return signals.length;
	}
	
	@Override
	public ISignal getOutput(int i) {
		return signals[i];
	}
	
	@Override
	public void onNewTick(long tick) {
		diffNode.tick(tick);
	}
	
	public enum SignalNames {
		FAST,
		SLOW,
		MACD,
		SIGNAL,
		DIFFERENCE,
	}
}
