package c3po.bitstamp;

import c3po.ISignal;
import c3po.node.INode;

public interface IBitstampTickerSource extends INode {
	public ISignal getOutputBid();
	public ISignal getOutputAsk();
	public ISignal getOutputVolume();
	public ISignal getOutputLast();
	public ISignal getOutputHigh();
	public ISignal getOutputLow();
}
