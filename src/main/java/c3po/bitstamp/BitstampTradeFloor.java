package c3po.bitstamp;

import java.util.Date;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import c3po.BootstrapTradeFloor;
import c3po.ISignal;
import c3po.IWallet;
import c3po.Sample;
import c3po.TradeAction;


public class BitstampTradeFloor extends BootstrapTradeFloor {
	private static final Logger LOGGER = LoggerFactory.getLogger(BitstampTradeFloor.class);
	private double tradeFee = 0.05d;
	
	private static int clientId = 665206;
	private static String apiKey = "eXD1rGdkzz77f8AtVnuRvkC5gNjYFPrm";
	private static String apiSecret = "Qatd3bQoMtpEbth6w8SQaEdNRoxRUfvV";

	
	public BitstampTradeFloor(ISignal last, ISignal bid, ISignal ask) {
		super(last, bid, ask);
	}
	
	/**
	 * Signature is a HMAC-SHA256 encoded message containing: nonce, client ID and API key. 
	 * The HMAC-SHA256 code must be generated using a secret key that was generated with your API key. 
	 * This code must be converted to it's hexadecimal representation (64 uppercase characters).
	 * 
	 * TODO A performance optimization can be had by initiating the sha256_HMAC only once. 
	 * 
	 * @return Signature
	 * @throws Exception
	 */
	public static String generateSignature() throws Exception {
		long nonce = new Date().getTime();
		String message = String.valueOf(nonce) + String.valueOf(clientId) + apiKey;
		
		// Initiate cipher with the apiSecret
		Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
		SecretKeySpec secret_key = new SecretKeySpec(apiSecret.getBytes(), "HmacSHA256");
		sha256_HMAC.init(secret_key);

		// Do the encryption
		byte[] encryptedMessage = sha256_HMAC.doFinal(message.getBytes());
		
		return Hex.encodeHexString(encryptedMessage).toUpperCase();
	}

	@Override
	public double buyImpl(IWallet wallet, TradeAction action) {
		// We get the latest ask, assuming the ticker is updated by some other part of the app
		Sample currentAsk = askSignal.peek();
				
		// The amount of Btc we are going to get if we buy for volume USD
		double boughtBtc = action.volume * (1.0d-tradeFee);
		double soldUsd = action.volume * currentAsk.value;
		
		// We assume the trade is fulfilled instantly, for the price of the ask
		wallet.transact(action.timestamp, -soldUsd, boughtBtc);
		
		return boughtBtc;
	}

	@Override
	public double sellImpl(IWallet wallet, TradeAction action) {
		// We get the latest ask, assuming the ticker is updated by some other part of the app
		Sample currentBid = bidSignal.peek();
		
		// We assume the trade is fulfilled instantly, for the price of the ask
		double boughtUsd = currentBid.value * (action.volume * (1.0d-tradeFee)); // volume in bitcoins
		double soldBtc = action.volume;
		
		wallet.transact(action.timestamp, boughtUsd, -soldBtc);
		
		return boughtUsd;
	}
}
