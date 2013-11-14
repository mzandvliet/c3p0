package c3po;

import java.util.List;

/* Todo: not done/happy with this */

public interface ISignalTransformer {
	public Sample transform(List<Sample> lastSignals, Sample newest);
}