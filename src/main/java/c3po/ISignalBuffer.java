package c3po;

import java.util.List;

public interface ISignalBuffer extends List<Sample>, ISignal  {
	public ISignal getSignal();
	public Sample getSample(long tick, int index);
	public Sample getInterpolatedSample(long tick, long timestamp);
}
