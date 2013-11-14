package c3po;

public interface ISignalSource extends ITickable {
	public int getNumSignals();
	public ISignal get(int i);
}
