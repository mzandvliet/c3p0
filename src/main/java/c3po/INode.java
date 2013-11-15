package c3po;

public interface INode extends ITickable {
	public int getNumOutputs();
	public ISignal getOutput(int i);
}
