package c3po;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class RealtimeClock implements IClock {
	private List<ITickable> tickables;
	private long timeStep;
	
	public RealtimeClock(long timeStep) {
		this.tickables = new ArrayList<ITickable>();
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
		
		// Todo: Whelp! How do we define our stop condition (user), and how do we check for it?
		
		//while (true) {
			long currentTime = new Date().getTime();
			for (ITickable tickable : tickables) {
				tickable.tick(currentTime);
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
