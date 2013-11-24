/**
 * 
 */
package c3po;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

/**
 * @author Joost
 *
 */
public class WalletTest {

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
	}

	/**
	 * Test method for {@link c3po.Wallet#Wallet(double, double)}.
	 */
	@Test
	public void testWallet() {
		double btc = 0.123123;
		double usd = 312312.41;
		Wallet wallet = new Wallet(usd, btc);
		assertEquals(btc, wallet.getWalletBtc(), 0.0001);
		assertEquals(usd, wallet.getWalletUsd(), 0.0001);
	}

	/**
	 * Test method for {@link c3po.Wallet#update(double, double)}.
	 */
	@Test
	public void testUpdate() {
		Wallet wallet = new Wallet(0.123123, 312312.41);
		
		// Update it with new values
		double btc = 123.1234;
		double usd = 89.123;
		wallet.update(usd, btc);
		
		assertEquals(btc, wallet.getWalletBtc(), 0.0001);
		assertEquals(usd, wallet.getWalletUsd(), 0.0001);
	}

}
