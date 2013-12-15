package c3po.production;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import c3po.*;
import c3po.clock.IRealtimeClock;
import c3po.utils.Time;

public class RealtimeClock implements IRealtimeClock, Runnable {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(RealtimeClock.class);
	
	private List<ITickable> listeners;
	private long timeStep; 
	private long preloadTime;
	private long interpolationTime;

	SimpleDateFormat sdf  = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");
	
	private boolean stopIt = false;

	private long maxTicks;
	
	/**
	 * Creates a clock that runs realtime
	 * 
	 * @param timeStep This should be at least as small as your fastest bot's desired timeStep
	 * @param preloadTime The amount of milliseconds you want to preload for filling buffers
	 */
	public RealtimeClock(long timeStep, long preloadTime, long interpolationTime) {
		this.listeners = new ArrayList<ITickable>();
		this.timeStep = timeStep;
		this.preloadTime = preloadTime;
		this.interpolationTime = interpolationTime;
	}
	
	public RealtimeClock(long timeStep, long preloadTime, long maxTicks, long interpolationTime) {
		this(timeStep, preloadTime, interpolationTime);
		this.maxTicks = maxTicks;
	}
	
	@Override
	public void addListener(ITickable tickable) {
		listeners.add(tickable);
	}

	@Override
	public void removeListener(ITickable tickable) {
		listeners.remove(tickable);
	}
	
	@Override
	public void run() {
		LOGGER.debug("Prewarming...");
		prewarm();
		LOGGER.debug("Finished preloading data, starting realtime execution");
		runRealtime();
	}
	
	private void prewarm() {
		// First run the learning period
		long systemStartTime = new Date().getTime();
		long currentTick = systemStartTime - preloadTime - interpolationTime;

		while(currentTick < systemStartTime - interpolationTime) {
			Date tickDate = new Date(currentTick);
			LOGGER.debug("Clock tick: " + Time.format(tickDate));
			
			for (ITickable tickable : listeners) {
				tickable.tick(currentTick);
			}
		
			currentTick += timeStep;
		}
	}
	
	private void runRealtime() {
		long tickIndex = 0;
		while (stopIt == false && (maxTicks == 0 || maxTicks > tickIndex)) {
			long currentTick = new Date().getTime() - interpolationTime;
			Date tickDate = new Date(currentTick);
			LOGGER.debug("Clock tick: " + Time.format(tickDate));
			for (ITickable tickable : listeners) {
				tickable.tick(currentTick);
			}

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
