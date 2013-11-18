package c3po;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import c3po.macd.RealBot;

public class RealtimeClock implements IClock, Runnable {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(RealtimeClock.class);
	
	private List<IBot> bots;
	private long timeStep; 
	private long preloadTime;
	
	/**
	 * Creates a clock that runs realtime
	 * 
	 * @param timeStep This should be at least as small as your fastest bot's desired timeStep
	 * @param preloadTime The amount of milliseconds you want to preload for filling buffers
	 */
	public RealtimeClock(long timeStep, long preloadTime) {
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
		
		// First run the learning period
		long currentTick = new Date().getTime() - preloadTime;
		while(currentTick < new Date().getTime())
		for (IBot bot : bots) {
			if (currentTick - bot.getLastTick() >= bot.getTimestep()) {
				bot.tick(currentTick);
			}
			
			currentTick += timeStep;
		}
		
		// And now... run forever!
		while (true) {
			currentTick = new Date().getTime();
			for (IBot bot : bots) {
				if (currentTick - bot.getLastTick() >= bot.getTimestep()) {
					bot.tick(currentTick);
				}
			}

			Wait(timeStep);
		}
	}
	
	private static void Wait(long interval) {
		try {
			Thread.sleep(interval);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
