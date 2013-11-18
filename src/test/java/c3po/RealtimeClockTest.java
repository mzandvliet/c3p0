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
	public void testRun() {
		
		RealtimeClock clock = new RealtimeClock(1000, 60000);
				
		IBot fastBot = mock(IBot.class);
		when(fastBot.getTimestep()).thenReturn(1000l);
		
		IBot slowBot = mock(IBot.class);
		when(fastBot.getTimestep()).thenReturn(2000l);
		
		
		new Thread(clock).start();
		
		
	}

}
