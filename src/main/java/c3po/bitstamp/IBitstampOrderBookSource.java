package c3po.bitstamp;

import c3po.ISignal;
import c3po.node.INode;

public interface IBitstampOrderBookSource extends INode {
	public ISignal getOutputVolumeBid();
	public ISignal getOutputVolumeAsk();
	public ISignal getOutputP99Bid();
	public ISignal getOutputP98Bid();
	public ISignal getOutputP97Bid();
	public ISignal getOutputP96Bid();
	public ISignal getOutputP95Bid();
	public ISignal getOutputP99Ask();
	public ISignal getOutputP98Ask();
	public ISignal getOutputP97Ask();
	public ISignal getOutputP96Ask();
	public ISignal getOutputP95Ask();
}
