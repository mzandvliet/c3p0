package c3po.bitstamp;

import static org.junit.Assert.*;

import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.util.Date;

import org.junit.Test;

import c3po.bitstamp.BitstampTickerDbSource;

public class BitstampTradefloorTest {

	private final static long timestep = 60000;
	private final static long interpolationTime = 120000;
	
	@Test
	public void testGenerateSignature() throws Exception {
		String signature = BitstampTradeFloor.generateSignature();
		
		// Signature must be 64 characters long
		assertEquals(64, signature.length());
		
		// Signature must be uppercase
		assertEquals(signature.toUpperCase(), signature);
	}

}
