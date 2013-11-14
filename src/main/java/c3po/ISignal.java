package c3po;

public interface ISignal extends ITickable {
	public Sample getSample(long tick);
}
