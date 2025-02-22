package c3po.bitstamp;

import static org.junit.Assert.*;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONObject;
import org.junit.Ignore;
import org.junit.Test;

import c3po.structs.TradeResult;

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
		assertEquals("1322.99", BitstampTradeFloor.doubleToPriceString(1322.99d));
		assertEquals("1322.99", BitstampTradeFloor.doubleToPriceString(1322.991d));
		assertEquals("1322.98", BitstampTradeFloor.doubleToPriceString(1322.989d));
		assertEquals("1322.98", BitstampTradeFloor.doubleToPriceString(001322.989d));
		assertEquals("322.98", BitstampTradeFloor.doubleToPriceString(322.989d));
	}
	
	@Test
	public void testAmountFormatter() {
		assertEquals("0.79758126", BitstampTradeFloor.doubleToAmountString(0.79758126d));
		assertEquals("0.79758126", BitstampTradeFloor.doubleToAmountString(0.797581266d));
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
		List<TradeResult> openOrders = tf.getOpenOrders();
		System.out.println(openOrders);
	}
	
	@Test
	public void testCalculateTradeFee() throws Exception {
		double tradefeeUsd = BitstampTradeFloor.calculateTradeFeeUsd(1222.37, 0.0022);
		assertEquals(2.69, tradefeeUsd, 0.00000001);
	}
	
	@Test
	public void testCalculateBtcToBuy() throws Exception {
		double calculateBtcToBuy = BitstampTradeFloor.calculateBtcToBuy(37.06, 578.97, 0.0044);
		assertEquals("0.0637166", BitstampTradeFloor.doubleToAmountString(calculateBtcToBuy));
	}
	
	@Test @Ignore
	public void realTestAdjust() throws Exception {
		BitstampTradeFloor tf = new BitstampTradeFloor(null, null, null, true);
		tf.adjustOrders();
	}
	
	@Test @Ignore
	public void realTest() throws Exception {
		BitstampTradeFloor tf = new BitstampTradeFloor(null, null, null, true);
		
		// Fetch the current open orders
		List<TradeResult> openOrders = tf.getOpenOrders();
		
		List<TradeResult> newOrders = new LinkedList<TradeResult>();
		
		// Add a buy order
		int buyPrice = 300;
		double buyAmount = 0.01;
		TradeResult buyOrder = tf.placeBuyOrder(buyPrice, buyAmount);
		newOrders.add(buyOrder);
		
		assertEquals(buyPrice, buyOrder.getPrice(), 0.001d);
		assertEquals(buyAmount, buyOrder.getAmount(), 0.001d);

		// Add a sell order
		int sellPrice = 10000;
		double sellAmount = 0.01;
		TradeResult sellOrder = tf.placeSellOrder(sellPrice, sellAmount);
		newOrders.add(sellOrder);
		
		assertEquals(sellPrice, sellOrder.getPrice(), 0.001d);
		assertEquals(sellAmount, sellOrder.getAmount(), 0.001d);

		Thread.sleep(60000);
		
		// See if the new orders arrived
		List<TradeResult> openOrders2 = tf.getOpenOrders();
		assertEquals(openOrders.size() + 2, openOrders2.size());
		
		// Cancel the orders again, check that there are none
		tf.cancelOrders(newOrders);
		
		Thread.sleep(60000);
		
		List<TradeResult> openOrders3 = tf.getOpenOrders();
		assertEquals(openOrders.size(), openOrders3.size());
	}
}
