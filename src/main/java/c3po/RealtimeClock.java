package c3po;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RealtimeClock implements IClock, Runnable {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(RealtimeClock.class);
	
	private List<IClockListener> listeners;
	private long timeStep; 
	private long preloadTime;
	private long interpolationTime;

	private boolean stopIt = false;

	private long maxTicks;
	
	/**
	 * Creates a clock that runs realtime
	 * 
	 * @param timeStep This should be at least as small as your fastest bot's desired timeStep
	 * @param preloadTime The amount of milliseconds you want to preload for filling buffers
	 */
	public RealtimeClock(long timeStep, long preloadTime, long interpolationTime) {
		this.listeners = new ArrayList<IClockListener>();
		this.timeStep = timeStep;
		this.preloadTime = preloadTime;
		this.interpolationTime = interpolationTime;
	}
	
	public RealtimeClock(long timeStep, long preloadTime, long maxTicks, long interpolationTime) {
		this(timeStep, preloadTime, interpolationTime);
		this.maxTicks = maxTicks;
	}
	
	@Override
	public void addListener(IClockListener tickable) {
		listeners.add(tickable);
	}

	@Override
	public void removeListener(IClockListener tickable) {
		listeners.remove(tickable);
	}
	
	public void run() {
		// Tick the leafs repeatedly to propagate (or 'draw') samples through the tree from roots to leaves
		
		// Todo: Whelp! How do we define our stop condition (user), and how do we check for it?
		
		// First run the learning period
		long currentTick = new Date().getTime() - preloadTime - interpolationTime;
		LOGGER.debug("Tick: " + currentTick);
		
		while(currentTick < new Date().getTime()) {
			for (IClockListener bot : listeners) {
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
			currentTick = new Date().getTime() - interpolationTime;
			
			for (IClockListener updatable : listeners) {
				if (updatable.getLastTick() == 0 || currentTick - updatable.getLastTick() >= updatable.getTimestep()) {
					updatable.tick(currentTick);
					LOGGER.debug("Bot : " + updatable + " Tick: " + currentTick);
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
