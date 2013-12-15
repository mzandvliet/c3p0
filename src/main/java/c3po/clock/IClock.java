package c3po.clock;

import c3po.ITickable;

public interface IClock {
	public void addListener(ITickable listener);
	public void removeListener(ITickable listener);
}
