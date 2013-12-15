package c3po.node;

import c3po.ISignal;
import c3po.ITickable;

public interface INode extends ITickable {
	public int getNumOutputs();
	public ISignal getOutput(int i);
}
