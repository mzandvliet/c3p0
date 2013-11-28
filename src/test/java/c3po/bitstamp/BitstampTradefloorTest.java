package c3po.bitstamp;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.LinkedList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import c3po.ISignal;

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
	public void testAuthenticatedCall() throws Exception {
		BitstampTradeFloor tf = new BitstampTradeFloor(null, null, null);
		JSONObject result = new JSONObject(tf.doAuthenticatedCall("https://www.bitstamp.net/api/balance/"));
		System.out.println(result);
		
		// This throws an exception if the authentication went wrong
		result.get("fee");
	}
	
	@Test
	public void testOpenOrders() throws Exception {
		BitstampTradeFloor tf = new BitstampTradeFloor(null, null, null);
		JSONArray result = new JSONArray(tf.doAuthenticatedCall("https://www.bitstamp.net/api/open_orders/"));
	}
	
	@Test
	public void testPlaceBuyOrder() throws Exception {
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

		assertEquals("TRUE", mock.placeBuyOrder(price, amount));
		
	}
	
	/**
	 * Class used to help testing the TradeFloor.
	 * Takes an callback which is returned on calling
	 * doAuthenticatedCall.
	 */
	private class BitstampTradeFloorMock extends BitstampTradeFloor {
		JSONCallback callback;
		public BitstampTradeFloorMock(JSONCallback callback) {
			super(null, null, null);
			this.callback = callback;
		}
		/* (non-Javadoc)
		 * @see c3po.bitstamp.BitstampTradeFloor#doAuthenticatedCall(java.lang.String, java.util.List)
		 */
		@Override
		public String doAuthenticatedCall(String url, List<NameValuePair> params) throws Exception {
			return callback.getData(url, params);
		}
	}
	
	private interface JSONCallback {
		public String getData(String url, List<NameValuePair> params);
	}
}
