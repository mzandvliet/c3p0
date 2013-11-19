package c3po;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RealtimeClock implements IClock, Runnable {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(RealtimeClock.class);
	
	private List<IBot> bots;
	private long timeStep; 
	private long preloadTime;

	private boolean stopIt = false;

	private long maxTicks;
	
	/**
	 * Creates a clock that runs realtime
	 * 
	 * @param timeStep This should be at least as small as your fastest bot's desired timeStep
	 * @param preloadTime The amount of milliseconds you want to preload for filling buffers
	 */
	public RealtimeClock(long timeStep, long preloadTime) {
		this.bots = new ArrayList<IBot>();
		this.timeStep = timeStep;
		this.preloadTime = preloadTime;
	}
	
	public RealtimeClock(long timeStep, long preloadTime, long maxTicks) {
		this(timeStep, preloadTime);
		this.maxTicks = maxTicks;
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
		LOGGER.debug("Tick: " + currentTick);
		while(currentTick < new Date().getTime()) {
			for (IBot bot : bots) {
				if (bot.getLastTick() == 0 || currentTick - bot.getLastTick() >= bot.getTimestep()) {
					bot.tick(currentTick);
					LOGGER.debug(" Tick: " + new Date(currentTick).toLocaleString() + " Bot : " + bot );
				}
			}
		
			currentTick += timeStep;
		}
		
		LOGGER.debug("Finished preloading data, starting realtime execution");
		
		// And now... run forever!
		long tickIndex = 0;
		while (stopIt == false && (maxTicks == 0 || maxTicks > tickIndex)) {
			currentTick = new Date().getTime();
			for (IBot bot : bots) {
				if (bot.getLastTick() == 0 || currentTick - bot.getLastTick() >= bot.getTimestep()) {
					bot.tick(currentTick);
					LOGGER.debug("Bot : " + bot + " Tick: " + currentTick);
				}
			}
			
			LOGGER.debug("Tick: " + currentTick);

			Wait(timeStep);
			tickIndex++;
		}
	}
	
	private static void Wait(long interval) {
		try {
			Thread.sleep(interval);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public synchronized void stop() {
		this.stopIt = true;
	}
}
