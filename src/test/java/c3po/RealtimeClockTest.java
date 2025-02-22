/**
 * 
 */
package c3po;

import org.junit.Test;

import c3po.production.RealtimeClock;
import c3po.wallet.IWallet;

/**
 * @author Joost
 *
 */
public class RealtimeClockTest {

	@Test
	public void testRun() throws InterruptedException {
		
		RealtimeClock clock = new RealtimeClock(1000, 60000, 4);
				
		// Add a slowbot
		IBot fastBot = new StepTickBot(1000);
		clock.addListener(fastBot);
		
		
		// Add a fastbot
		IBot slowBot = new StepTickBot(2000);
		clock.addListener(slowBot);
		
		
		new Thread(clock).start();
		
		// Wait for the two ticks
		Thread.sleep(6000);
		
		clock.stop();
	}
	

	/**
	 * Test bot that only does stepping and ticking, its so stupid
	 */
	public class StepTickBot implements IBot {

		long lastTick;
		long timestep;
		
		public StepTickBot(long timestep) {
			this.timestep = timestep;
		}
		@Override
		public long getLastTick() {
			return lastTick;
		}

		@Override
		public void tick(long tick) {
			this.lastTick = tick;
		}

		@Override
		public long getTimestep() {
			return timestep;
		}
		
		@Override
		public IWallet getWallet() {
			// TODO Auto-generated method stub
			return null;
		}
		@Override
		public ITradeFloor getTradeFloor() {
			return null;
		}
		@Override
		public void addTradeListener(ITradeListener listener) {
			// TODO Auto-generated method stub
			
		}
		@Override
		public void removeListener(ITradeListener listener) {
			// TODO Auto-generated method stub
			
		}
		@Override
		public IBotConfig getConfig() {
			// TODO Auto-generated method stub
			return null;
		}
		@Override
		public int getId() {
			// TODO Auto-generated method stub
			return 0;
		}
	}

}
