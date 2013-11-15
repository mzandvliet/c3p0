package c3po;

/* 
 * MacdSource
 * 
 * This is kind of a macro, a signal tree wrapped in a single node for easy use, with
 * defined inputs and outputs for hooking it into other signal trees.
 * 
 * Todo:
 * 
 * - Create internal signal tree
 * - Clear definition of inputs and outputs
 * - Expose node tree for manual introspection (like chart drawing of buffers or visualizing structure)
 */
public class MacdNode implements INode {
	private final int numSignals = 5;
	private ISignal[] signals;
	private long lastTick = -1;
	
	INode fastNode;
	INode slowNode;
	INode macdNode;
	INode signalNode;
	INode diffNode;
	
	public MacdNode(ISignal ticker, int fast, int slow, int signal) {
		this.signals = new SurrogateSignal[numSignals];
		
		// Create internal signal tree, hook all intermediate results up to outputs
		
		fastNode = new ExpMovingAverageNode(ticker, fast);
		slowNode = new ExpMovingAverageNode(ticker, slow);
		macdNode = new SubtractNode(fastNode.getOutput(0), slowNode.getOutput(0));
		signalNode = new ExpMovingAverageNode(macdNode.getOutput(0), signal);
		diffNode = new SubtractNode(macdNode.getOutput(0), signalNode.getOutput(0));
		
		this.signals[0] = fastNode.getOutput(0);
		this.signals[1] = slowNode.getOutput(0);
		this.signals[2] = macdNode.getOutput(0);
		this.signals[3] = signalNode.getOutput(0);
		this.signals[4] = diffNode.getOutput(0);
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
