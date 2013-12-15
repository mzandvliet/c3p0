package c3po.node;

import c3po.AbstractTickable;
import c3po.ISignal;
import c3po.OutputSignal;
import c3po.Sample;

/*
 * Todo: Implement, lol
 */

public class VolatilityNode extends AbstractTickable implements INode {
	private ISignal input;
	private OutputSignal output;
	
	public VolatilityNode(long timestep, ISignal input) {
		super(timestep);
		this.input = input;
		this.output = new OutputSignal(this);
	}

	@Override
	public int getNumOutputs() {
		return 1;
	}

	@Override
	public ISignal getOutput(int i) {
		return output;
	}
	
	@Override
	public void onNewTick(long tick) {
		output.setSample(Sample.none);
	}
}
