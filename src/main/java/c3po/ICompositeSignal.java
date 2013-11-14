package c3po;

public interface ICompositeSignal extends ITickable {
	public int getNumSignals();
	public ISignal get(int i);
}
