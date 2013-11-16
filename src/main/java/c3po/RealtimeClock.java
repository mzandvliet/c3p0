package c3po;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class RealtimeClock implements IClock {
	private List<IBot> bots;
	private long timeStep; // This should be at least as small as your fastest bot's desired timeStep
	
	public RealtimeClock(long timeStep) {
		this.bots = new ArrayList<IBot>();
		this.timeStep = timeStep;
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
		// Tick the leafs repeatedly to propagate (or 'draw') samples through the tree from roots to leaves
		
		// Todo: Whelp! How do we define our stop condition (user), and how do we check for it?
		
		//while (true) {
			long currentTick = new Date().getTime();
			for (IBot bot : bots) {
				if (currentTick - bot.getLastTick() >= bot.getTimestep()) {
					bot.tick(currentTick);
				}
			}
			
			Wait(timeStep);
		//}
	}
	
	private static void Wait(long interval) {
		try {
			Thread.sleep(interval);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
