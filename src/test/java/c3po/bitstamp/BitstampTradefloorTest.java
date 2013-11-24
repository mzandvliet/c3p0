package c3po.bitstamp;

import static org.junit.Assert.*;

import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.util.Date;

import org.json.JSONObject;
import org.junit.Test;

import c3po.bitstamp.BitstampTickerDbSource;

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
		JSONObject result = BitstampTradeFloor.doAuthenticatedCall("https://www.bitstamp.net/api/balance/");
		System.out.println(result);
		
		// This throws an exception if the authentication went wrong
		result.get("fee");
	}
}
