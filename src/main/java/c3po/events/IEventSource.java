package c3po.events;

public interface IEventSource<TEvent> {
	public void addListener(IEventListener<TEvent> listener);
	public void removeListener(IEventListener<TEvent> listener);
}