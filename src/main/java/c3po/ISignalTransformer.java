package c3po;

import java.util.List;

public interface ISignalTransformer {
	public Signal transform(List<Signal> lastSignals, Signal newest);
}
