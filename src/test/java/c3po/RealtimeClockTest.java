/**
 * 
 */
package c3po;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.*;

/**
 * @author Joost
 *
 */
public class RealtimeClockTest {


	@Test
	public void testRun() throws InterruptedException {
		
		RealtimeClock clock = new RealtimeClock(1000, 60000);
				
		// Add a slowbot
		IBot fastBot = mock(IBot.class);
		when(fastBot.getTimestep()).thenReturn(1000l);
		clock.addListener(fastBot);
		
		
		// Add a fastbot
		IBot slowBot = mock(IBot.class);
		when(fastBot.getTimestep()).thenReturn(2000l);
		clock.addListener(slowBot);
		
		
		new Thread(clock).start();
		
		// Wait for the two ticks
		Thread.sleep(2000);
		
		clock.stop();
		
		verify(fastBot, times(2)).tick(anyLong());
	}

}
