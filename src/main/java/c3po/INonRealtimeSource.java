package c3po;

import c3po.node.INode;

public interface INonRealtimeSource extends INode, IResetable {
	public long getStartTime();
	public long getEndTime();
	public void initializeForTimePeriod(long startTime, long endTime);
}
