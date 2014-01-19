/**
 * 
 */
package c3po;

import static org.junit.Assert.*;

import org.junit.Test;

import c3po.structs.OpenOrder;
import c3po.utils.Time;
import c3po.wallet.IWallet;

/**
 * @author Joost
 *
 */
public class AbstractTradeFloorTest {

	/**
	 * Test method for {@link c3po.AbstractTradeFloor#allowedToTrade(long)}.
	 */
	@Test
	public void testAllowedToTrade() {
		ITradeFloor tf = new AbstractTradeFloor(null, null, null, false) {
			
			@Override
			public void updateWallet(IWallet wallet) {
			}
			
			@Override
			protected OpenOrder sellImpl(long tick, IWallet wallet, TradeIntention action) {
				return null;
			}
			
			@Override
			protected OpenOrder buyImpl(long tick, IWallet wallet, TradeIntention action) {
				return null;
			}
		};
	
		// Try to buy the first time
		assertEquals(true, tf.allowedToTrade(1390000000000l));
		tf.buy(1390000000000l, null, null);
		
		// Try to sell a minute later
		assertEquals(false, tf.allowedToTrade(1390000000000l + (Time.MINUTES * 1)));
		assertNull(tf.sell(1390000000000l + (Time.MINUTES * 1), null, null));

		// Try to sell 10 minutes later
		assertEquals(true, tf.allowedToTrade(1390000000000l + (Time.MINUTES * 10)));
		tf.sell(1390000000000l + (Time.MINUTES * 10), null, null);	
	}
}
