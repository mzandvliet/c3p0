package c3po.events;

public interface IEventListener<TEvent> {
	public void onEvent(TEvent event);
}
