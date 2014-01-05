package c3po.events;

import java.util.Set;
import java.util.HashSet;

public class AbstractEventSource<TEvent> implements IEventSource<TEvent> {
	private final Set<IEventListener<TEvent>> listeners;
	
	public AbstractEventSource() {
		this.listeners = new HashSet<IEventListener<TEvent>>();
	}

	@Override
	public void addListener(IEventListener<TEvent> listener) {
		listeners.add(listener);
	}

	@Override
	public void removeListener(IEventListener<TEvent> listener) {
		listeners.remove(listener);
	}
	
	public void produce(TEvent event) {
		for (IEventListener<TEvent> listener : listeners) {
			listener.onEvent(event);
		}
	}
}
