package c3po;

import java.util.ArrayList;
import java.util.List;

/* 
 * Todo:
 * 
 *  - We want to base the tick's timestamp on the time defined in the simulation data.
 */

public class SimulationClock implements IClock {
	private List<ITickable> tickables;
	private long duration;
	private long timeStep;
	
	public SimulationClock(long duration, long timeStep) {
		this.tickables = new ArrayList<ITickable>();
		this.duration = duration;
		this.timeStep = timeStep;
	}
	
	@Override
	public void addListener(ITickable tickable) {
		tickables.add(tickable);
	}

	@Override
	public void removeListener(ITickable tickable) {
		tickables.remove(tickable);
	}

	public void run() {
		// Tick the leafs repeatedly to propagate (or 'draw') samples through the tree from roots to leaves

		for (long tick = 0; tick < duration; tick+=timeStep) {
			for (ITickable tickable : tickables) {
				tickable.tick(tick);
			}
		}
	}
}
