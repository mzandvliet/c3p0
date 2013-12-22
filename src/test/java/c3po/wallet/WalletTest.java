package c3po.wallet;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Date;

import org.junit.Test;

import c3po.wallet.Wallet;

public class WalletTest {

	/**
	 * Test method for {@link c3po.wallet.Wallet#Wallet(double, double)}.
	 */
	@Test
	public void testWallet() {
		// Test values
		double btcAvailable = 0.123123;
		double usdAvailable = 312312.41;
		double usdReserved = 12.123123;
		double btcReserved = 1.41;
		
		// Initiate test object
		Wallet wallet = new Wallet(usdAvailable, btcAvailable, usdReserved, btcReserved);
		
		// Validate contents
		assertEquals(btcAvailable, wallet.getBtcAvailable(), 0.0001);
		assertEquals(usdAvailable, wallet.getUsdAvailable(), 0.0001);
		assertEquals(btcReserved, wallet.getBtcReserved(), 0.0001);
		assertEquals(usdReserved, wallet.getUsdReserved(), 0.0001);
		assertEquals(btcAvailable+btcReserved, wallet.getBtcTotal(), 0.0001);
		assertEquals(usdAvailable+usdReserved, wallet.getUsdTotal(), 0.0001);
	}

	/**
	 * Test method for {@link c3po.wallet.Wallet#update(long double, double, double, double)}.
	 */
	@Test
	public void testUpdate() {
		// Create wallet and listener
		Wallet wallet = new Wallet(0.1, 2.41, 0.2, 0.4);
		IWalletUpdateListener listener = mock(IWalletUpdateListener.class);
		wallet.addListener(listener);
		
		long timestamp = new Date().getTime();
		double usdAvailable = 312312.41;
		double btcAvailable = 0.123123;
		double usdReserved = 12.123123;
		double btcReserved = 1.41;
		
		// Update it with new values
		wallet.update(timestamp, usdAvailable, btcAvailable, usdReserved, btcReserved);
		
		assertEquals(btcAvailable, wallet.getBtcAvailable(), 0.0001);
		assertEquals(usdAvailable, wallet.getUsdAvailable(), 0.0001);
		assertEquals(btcReserved, wallet.getBtcReserved(), 0.0001);
		assertEquals(usdReserved, wallet.getUsdReserved(), 0.0001);
		assertEquals(btcAvailable+btcReserved, wallet.getBtcTotal(), 0.0001);
		assertEquals(usdAvailable+usdReserved, wallet.getUsdTotal(), 0.0001);
		
		// Make sure the listener was called with an update
		verify(listener, times(1)).onWalletUpdate(any(WalletUpdateResult.class));
	}
	
	/**
	 * Test method for {@link c3po.wallet.Wallet#modify(double, double)}.
	 */
	@Test
	public void testModify() {
		// Create wallet and listener
		double usdAvailable = 1.1;
		double btcAvailable = 1.2;
		Wallet wallet = new Wallet(usdAvailable, btcAvailable, 0.3, 0.4);
		IWalletUpdateListener listener = mock(IWalletUpdateListener.class);
		wallet.addListener(listener);
		
		long timestamp = new Date().getTime();
		double usdModify = -0.6;
		double btcModify = 0.6;
		
		// Update it with new values
		wallet.modify(timestamp, usdModify, btcModify);
		
		assertEquals(usdAvailable + usdModify, wallet.getUsdAvailable(), 0.0001);
		assertEquals(btcAvailable + btcModify, wallet.getBtcAvailable(), 0.0001);

		
		// Make sure the listener was called with an update
		verify(listener, times(1)).onWalletUpdate(any(WalletUpdateResult.class));
	}
	
	/**
	 * Test method for {@link c3po.wallet.Wallet#reserve(double, double)}.
	 * @throws Exception 
	 */
	@Test
	public void testReserved() throws Exception {
		// Create wallet and listener
		double usdAvailable = 1.1;
		double btcAvailable = 1.2;
		double initialUsdReserve = 0.3;
		double initialBtcReserve = 0.4;
		Wallet wallet = new Wallet(usdAvailable, btcAvailable, initialUsdReserve, initialBtcReserve);
		IWalletUpdateListener listener = mock(IWalletUpdateListener.class);
		wallet.addListener(listener);
		
		double usdReserved = 0.2;
		double btcReserved = 0.6;
		
		// Update it with new values
		wallet.reserve(usdReserved, btcReserved);
		
		assertEquals(usdAvailable - usdReserved, wallet.getUsdAvailable(), 0.0001);
		assertEquals(btcAvailable - btcReserved, wallet.getBtcAvailable(), 0.0001);
		assertEquals(initialUsdReserve + usdReserved, wallet.getUsdReserved(), 0.0001);
		assertEquals(initialBtcReserve + btcReserved, wallet.getBtcReserved(), 0.0001);
		
		// Make sure the listener was not called with an update
		verify(listener, times(0)).onWalletUpdate(any(WalletUpdateResult.class));
	}
	
	/**
	 * Test method for {@link c3po.wallet.Wallet#reserve(double, double)}.
	 * @throws Exception 
	 */
	@Test(expected = Exception.class)
	public void testReservedExceptionUsd() throws Exception {
		Wallet wallet = new Wallet(1, 1, 0.5, 0.5);
		
		// Reserve too much USD
		wallet.reserve(1.1, 0);
	}
	
	/**
	 * Test method for {@link c3po.wallet.Wallet#reserve(double, double)}.
	 * @throws Exception 
	 */
	@Test(expected = Exception.class)
	public void testReservedExceptionBtc() throws Exception {
		Wallet wallet = new Wallet(1, 1, 0.5, 0.5);
		
		// Reserve too much Btc
		wallet.reserve(0, 1.1);
	}

}
