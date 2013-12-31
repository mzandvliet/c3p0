package c3po.bitstamp;

import c3po.ISignal;
import c3po.node.INode;

public interface IBitstampOrderBookSource extends INode {
	public ISignal getOutputVolumeBid();
	public ISignal getOutputVolumeAsk();
	public ISignal getOutputBidPercentile(int percentileIndex);
	public ISignal getOutputAskPercentile(int percentileIndex);
}
