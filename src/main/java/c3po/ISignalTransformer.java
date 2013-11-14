package c3po;

import java.util.List;

public interface ISignalTransformer {
	public Sample transform(List<Sample> lastSignals, Sample newest);
}