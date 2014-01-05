package c3po.orderbook;

import c3po.ISignal;
import c3po.node.INode;

public interface IOrderBookPercentileTransformer extends INode {
	public ISignal getOutputBidPercentile(int percentileIndex);
	public ISignal getOutputAskPercentile(int percentileIndex);
}
