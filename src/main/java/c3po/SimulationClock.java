package c3po;

import java.util.ArrayList;
import java.util.List;

/* 
 * Clocks should tick at a rate that is not specific to any bot (much faster, in fact).
 * Each clock iteration we check whether any bots want to be updated.
 */

public class SimulationClock implements IClock {
	private List<IBot> bots;
	private long startTime;
	private long endTime;
	private long clockTimestep;
	
	public SimulationClock(long clockTimestep, long startTime, long endTime) {
		this.bots = new ArrayList<IBot>();
		this.clockTimestep = clockTimestep;
		this.startTime = startTime;
		this.endTime = endTime;
	}
	
	@Override
	public void addListener(IBot tickable) {
		bots.add(tickable);
	}

	@Override
	public void removeListener(IBot tickable) {
		bots.remove(tickable);
	}

	public void run() {
		for (long currentTick = startTime; currentTick < endTime; currentTick += clockTimestep) {
			
			// Iterate over all tickables, see which needs to be ticked
			
			for (IBot bot : bots) {
				if (currentTick - bot.getLastTick() >= bot.getTimestep()) {
					bot.tick(currentTick);
				}
			}
		}
	}
}
