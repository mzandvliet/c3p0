package c3po;

import java.util.ArrayList;
import java.util.List;

/* 
 * Clocks should tick at a rate that is not specific to any bot (much faster, in fact).
 * Each clock iteration we check whether any bots want to be updated.
 */

public class SimulationClock implements ISimulationClock {
	private final List<ITickable> listeners;
	private final long clockTimestep;
	private final long interpolationTime;
	
	public SimulationClock(long clockTimestep, long interpolationTime) {
		this.listeners = new ArrayList<ITickable>();
		this.clockTimestep = clockTimestep;
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

	@Override
	public void run(long startTime, long endTime) {
		long delayedStartTime = startTime - interpolationTime;
		long delayedEndTime = endTime - interpolationTime;
		
		for (long currentTick = delayedStartTime; currentTick < delayedEndTime; currentTick += clockTimestep) {
			// Iterate over all tickables, see which needs to be ticked
			
			for (ITickable listener : listeners) {
				listener.tick(currentTick);
			}
		}
	}
}
