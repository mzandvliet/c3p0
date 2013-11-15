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
	private SurrogateSignal[] signals;
	private long lastTick = -1;
	
	public MacdNode(ISignal ticker, int fast, int slow, int signal) {
		this.signals = new SurrogateSignal[numSignals];
		
		/* Todo: 
		 * 
		 * - create internal signal tree (mov. avg. etc.), hook them up together
		 * 
		 * From the javascript original:
		 * 
		 * var avgA = filterExpMovingAverage_(ticker, fast);
  		 * var avgB = filterExpMovingAverage_(ticker, slow);
  		 * var macd = combine_(avgA, avgB, function(a, b) { return a - b; });
  		 * var macdAvg = filterExpMovingAverage_(macd, signal);
  		 * var diff = combine_(macd, macdAvg, function(a, b) { return a - b; });
		 */
		
		this.signals[0] = new SurrogateSignal(this);
		this.signals[1] = new SurrogateSignal(this);
		this.signals[2] = new SurrogateSignal(this);
		this.signals[3] = new SurrogateSignal(this);
		this.signals[4] = new SurrogateSignal(this);
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
			calculate();
			lastTick = tick;
		}
	}
	
	private void calculate() {
		/*
		 * Todo:
		 *  
		 * - Draw new Sample through internal signal tree
		 * - Send Sample to output signals
		 */
	}
	
	public enum SignalNames {
		FAST,
		SLOW,
		MACD,
		SIGNAL,
		DIFFERENCE
	}
	
	
}
