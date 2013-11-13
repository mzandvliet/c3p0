package c3po;

import java.util.List;

public interface ISignalBuffer extends List<Signal>, ISignalSource  {
	public Signal get(long tick, int index);
	public Signal getInterpolated(long tick, long timestamp);
	public Signal getLatest(long tick);
	public Signal getOldest(long tick);
}
