package c3po;

public interface ISignal {
	public String getName();
	public Sample getSample(long tick); // Get a Sample, triggering an update if required
}
