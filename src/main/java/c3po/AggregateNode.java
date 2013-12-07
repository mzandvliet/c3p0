package c3po;

public class AggregateNode extends AbstractTickable implements INode {
	private final ISignal[] inputs;
	private OutputSignal output;
	
	public AggregateNode(long timestep, ISignal ... inputs) {
		super(timestep);
		this.inputs = inputs;
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
		long aggregateTimestamp = 0;
		double aggregateValue = 0d;
		for (ISignal signal : inputs) {
			Sample sample = signal.getSample(tick);
			aggregateTimestamp += sample.timestamp;
			aggregateValue += sample.value;
		}
		output.setSample(new Sample(aggregateTimestamp / inputs.length, aggregateValue / (double)inputs.length));
	}
}
