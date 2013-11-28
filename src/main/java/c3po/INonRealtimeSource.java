package c3po;

public interface INonRealtimeSource extends INode {
	public long getStartTime();
	public long getEndTime();
	public void reset();
}
