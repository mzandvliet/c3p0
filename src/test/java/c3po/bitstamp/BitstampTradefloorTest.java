package c3po.bitstamp;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Ignore;
import org.junit.Test;

import c3po.ISignal;
import c3po.structs.OpenOrder;

public class BitstampTradefloorTest {

	private final static long timestep = 60000;
	private final static long interpolationTime = 120000;
	
	@Test
	public void testGenerateSignature() throws Exception {
		long nonce = BitstampTradeFloor.generateNonce();
		String signature = BitstampTradeFloor.generateSignature(nonce);
		
		System.out.println("Generated sig " + signature + " for nonce " + nonce);
		
		// Signature must be 64 characters long
		assertEquals(64, signature.length());
		
		// Signature must be uppercase
		assertEquals(signature.toUpperCase(), signature);
	}
	
	@Test
	public void testCurrencyFormatter() {
		assertEquals("1322.99", BitstampTradeFloor.doubleToCurrencyString(1322.99d));
		assertEquals("1322.99", BitstampTradeFloor.doubleToCurrencyString(1322.991d));
		assertEquals("1322.99", BitstampTradeFloor.doubleToCurrencyString(1322.989d));
		assertEquals("1322.99", BitstampTradeFloor.doubleToCurrencyString(001322.989d));
		assertEquals("322.99", BitstampTradeFloor.doubleToCurrencyString(322.989d));
	}
	
	@Test
	public void testAuthenticatedCall() throws Exception {
		BitstampTradeFloor tf = new BitstampTradeFloor(null, null, null, true);
		JSONObject result = new JSONObject(tf.doAuthenticatedCall("https://www.bitstamp.net/api/balance/"));
		System.out.println(result);
		
		// This throws an exception if the authentication went wrong
		result.get("fee");
	}
	
	@Test
	public void testOpenOrders() throws Exception {
		BitstampTradeFloor tf = new BitstampTradeFloor(null, null, null, true);
		List<OpenOrder> openOrders = tf.getOpenOrders();
		System.out.println(openOrders);
	}
	
	@Test
	public void testPlaceBuyOrder() throws Exception {
		/*
		final BitstampTradeFloor mock = new BitstampTradeFloorMock(new JSONCallback() {		
			@Override
			public String getData(String url, List<NameValuePair> params) {
				if(url == "https://www.bitstamp.net/api/buy/" && params.size() == 2) {
					return "TRUE";
				} else {
					return "FALSE";
				}
			}
		});
		
	    double price = 308.55;
		double amount = 0.0001;
		
		mock.placeBuyOrder(price, amount);
*/
		
		
	}
	
	@Test
	public void realTestAdjust() throws Exception {
		BitstampTradeFloor tf = new BitstampTradeFloor(null, null, null, true);
		tf.adjustOrders();
	}
	
	@Test @Ignore
	public void realTest() throws Exception {
		BitstampTradeFloor tf = new BitstampTradeFloor(null, null, null, true);
		
		// Fetch the current open orders
		List<OpenOrder> openOrders = tf.getOpenOrders();
		
		List<OpenOrder> newOrders = new LinkedList<OpenOrder>();
		
		// Add a buy order
		int buyPrice = 300;
		double buyAmount = 0.01;
		OpenOrder buyOrder = tf.placeBuyOrder(buyPrice, buyAmount);
		newOrders.add(buyOrder);
		
		assertEquals(buyPrice, buyOrder.getPrice(), 0.001d);
		assertEquals(buyAmount, buyOrder.getAmount(), 0.001d);

		// Add a sell order
		int sellPrice = 10000;
		double sellAmount = 0.01;
		OpenOrder sellOrder = tf.placeSellOrder(sellPrice, sellAmount);
		newOrders.add(sellOrder);
		
		assertEquals(sellPrice, sellOrder.getPrice(), 0.001d);
		assertEquals(sellAmount, sellOrder.getAmount(), 0.001d);

		Thread.sleep(60000);
		
		// See if the new orders arrived
		List<OpenOrder> openOrders2 = tf.getOpenOrders();
		assertEquals(openOrders.size() + 2, openOrders2.size());
		
		// Cancel the orders again, check that there are none
		tf.cancelOrders(newOrders);
		
		Thread.sleep(60000);
		
		List<OpenOrder> openOrders3 = tf.getOpenOrders();
		assertEquals(openOrders.size(), openOrders3.size());
	}
	
	
	
	/**
	 * Class used to help testing the TradeFloor.
	 * Takes an callback which is returned on calling
	 * doAuthenticatedCall.
	 */
	private class BitstampTradeFloorMock extends BitstampTradeFloor {
		JSONCallback callback;
		public BitstampTradeFloorMock(JSONCallback callback) {
			super(null, null, null, true);
			this.callback = callback;
		}

		@Override
		public String doAuthenticatedCall(String url, List<NameValuePair> params) throws Exception {
			return callback.getData(url, params);
		}
	}
	
	private interface JSONCallback {
		public String getData(String url, List<NameValuePair> params);
	}
}
