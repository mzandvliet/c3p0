package c3po;

import c3po.node.INode;

/*
 * Used to encapsulate a single output value for a Node
 * 
 * Ticks its parent when queried for new value. Parent should then supply it with latest Sample.
 */
public class OutputSignal implements ISignal {
	private INode ownerNode;
	private String name;
	private Sample latestSample = Sample.none;
	
	public OutputSignal(INode node, String name) {
		this.ownerNode = node;
		this.name = name;
	}
	
	public void setSample(Sample sample) {
		latestSample = sample;
	}
	
	@Override
	public Sample peek() {
		return latestSample;
	}
	
	@Override
	public Sample getSample(long tick) {
		ownerNode.tick(tick);
		return Sample.copy(latestSample);
	}
	
	public String toString() {
		return String.format("%s from %s", latestSample, ownerNode);
	}

	@Override
	public String getName() {
		return this.name;
	}
}