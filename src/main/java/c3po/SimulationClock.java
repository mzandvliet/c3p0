package c3po;

import java.util.ArrayList;
import java.util.List;

/* 
 * Clocks should tick at a rate that is not specific to any bot (much faster, in fact).
 * Each clock iteration we check whether any bots want to be updated.
 */

public class SimulationClock implements IClock {
	private final List<ITickable> listeners;
	private final long startTime;
	private final long endTime;
	private final long clockTimestep;
	private final long interpolationTime;
	
	public SimulationClock(long clockTimestep, long startTime, long endTime, long interpolationTime) {
		this.listeners = new ArrayList<ITickable>();
		this.clockTimestep = clockTimestep;
		this.startTime = startTime - interpolationTime; // Delay the client's time
		this.endTime = endTime - interpolationTime;
		this.interpolationTime = interpolationTime;
	}
	
	@Override
	public void addListener(ITickable listener) {
		listeners.add(listener);
	}

	@Override
	public void removeListener(ITickable listener) {
		listeners.remove(listener);
	}

	public void run() {
		for (long currentTick = startTime; currentTick < endTime; currentTick += clockTimestep) {
			// Iterate over all tickables, see which needs to be ticked
			
			for (ITickable listener : listeners) {
				listener.tick(currentTick);
			}
		}
	}
}
