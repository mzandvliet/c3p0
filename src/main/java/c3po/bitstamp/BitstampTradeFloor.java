package c3po.bitstamp;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Hex;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import c3po.AbstractTradeFloor;
import c3po.ISignal;
import c3po.IWallet;
import c3po.JsonReader;
import c3po.Sample;
import c3po.Time;
import c3po.TradeAction;
import c3po.structs.OpenOrder;


public class BitstampTradeFloor extends AbstractTradeFloor {
	private static final Logger LOGGER = LoggerFactory.getLogger(BitstampTradeFloor.class);
	private double tradeFee = 0.05d;
	
	private static int clientId = 665206;
	private static String apiKey = "8C3i5RNNZ3Hvy3epS7TKRp87a3K6tX4s";
	private static String apiSecret = "ZPS0qszXKqWtOM8PeTiLrqJuCOjLI3rl";
	
	SimpleDateFormat sdf  = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");
	private long lastWalletUpdate;
	
	public BitstampTradeFloor(ISignal last, ISignal bid, ISignal ask) {
		super(last, bid, ask);
		
		// Real stuff is happening, extra JSON logging
		JsonReader.debug = true;
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
	public static String generateSignature(long nonce) throws Exception {
		String message = String.valueOf(nonce) + String.valueOf(clientId) + apiKey;
		
		// Initiate cipher with the apiSecret
		Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
		SecretKeySpec secret_key = new SecretKeySpec(apiSecret.getBytes(), "HmacSHA256");
		sha256_HMAC.init(secret_key);

		// Do the encryption
		byte[] encryptedMessage = sha256_HMAC.doFinal(message.getBytes());
		
		return Hex.encodeHexString(encryptedMessage).toUpperCase();
	}
	
	public static long generateNonce() {
		return new Date().getTime();
	}
	
	public String doAuthenticatedCall(String url) throws Exception {
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		return doAuthenticatedCall(url, params);
	}
	
	public String doAuthenticatedCall(String url, List<NameValuePair> params) throws Exception {	
		long nonce = generateNonce();
		String sig = generateSignature(nonce);
		
		params.add(new BasicNameValuePair("key", apiKey));
		params.add(new BasicNameValuePair("nonce", String.valueOf(nonce)));
		params.add(new BasicNameValuePair("signature", sig));
		return JsonReader.readJsonFromUrl(url, params);
	}

	@Override
	public double buyImpl(IWallet wallet, TradeAction action) {
		double boughtBtc = 0;
		try {
			// We get the latest ask, assuming the ticker is updated by some other part of the app
			Sample currentAsk = askSignal.peek();
					
			// The amount of Btc we are going to get if we buy for volume USD
			boughtBtc = action.volume * (1.0d-tradeFee);
			double soldUsd = action.volume * currentAsk.value;
			
			// Place the actual buy order
			placeBuyOrder(currentAsk.value, boughtBtc);
			
			// We assume the trade is fulfilled instantly, for the price of the ask
			wallet.transact(action.timestamp, -soldUsd, boughtBtc);
		}
		catch(Exception e) {
			LOGGER.error("Could not buy BTC", e);
		}
		
		return boughtBtc;
	}

	@Override
	public double sellImpl(IWallet wallet, TradeAction action) {
		double boughtUsd = 0;
		try {
			// We get the latest ask, assuming the ticker is updated by some other part of the app
			Sample currentBid = bidSignal.peek();
			
			// We assume the trade is fulfilled instantly, for the price of the ask
			boughtUsd = currentBid.value * (action.volume * (1.0d-tradeFee)); // volume in bitcoins
			double soldBtc = action.volume;
			
			// Place the actual sell order
			placeSellOrder(currentBid.value, soldBtc);
			
			wallet.transact(action.timestamp, boughtUsd, -soldBtc);
		
		}
		catch(Exception e) {
			LOGGER.error("Could not sell BTC", e);
		}
		
		return boughtUsd;
	}
	
	@Override
	public void updateWallet(IWallet wallet) {
		
		if(lastWalletUpdate + 1 * Time.MINUTES < new Date().getTime()) {
			try {
				JSONObject result = new JSONObject(doAuthenticatedCall("https://www.bitstamp.net/api/balance/"));
				wallet.update(result.getDouble("usd_available"), result.getDouble("btc_available"));
				
				// Update tradeFee if needed
				if(result.getDouble("fee") != tradeFee) {
					LOGGER.info("Updated tradeFee. Old: " + tradeFee + " New: " + result.getDouble("fee"));
					tradeFee = result.getDouble("fee");
				}
			} catch (Exception e) {
				LOGGER.error("Could not update wallet", e);
			}
			
			lastWalletUpdate = new Date().getTime();
		}
	}

	
	/**
	 * This method looks at the currently open orders of the bot
	 * and readjusts them if the prices are outdated. This makes
	 * sure open orders are filled as soon as possible.
	 */
	public void adjustOrders() {
		try {
			Sample currentAsk = this.askSignal.peek();
			Sample currentBid = this.bidSignal.peek();
			
			// Loop over all the open orders
			for(OpenOrder openOrder : getOpenOrders()) {
				// Adjust sell order if needed
				if(openOrder.getType() == OpenOrder.SELL && openOrder.getPrice() != currentBid.value) {
					LOGGER.info("Adjusting "+ openOrder + " to match price " + currentBid.value);
					cancelOrder(openOrder);
					placeSellOrder(currentBid.value, openOrder.getAmount());
				}
				
				// Adjust buy order if needed
				if(openOrder.getType() == OpenOrder.BUY && openOrder.getPrice() != currentAsk.value) {
					LOGGER.info("Adjusting "+ openOrder + " to match price " + currentBid.value);
					cancelOrder(openOrder);
					placeBuyOrder(currentAsk.value, openOrder.getAmount());
				}
			}
			
		} catch(Exception e) {
			LOGGER.error("Could not adjust orders", e);
		}
	}
	

	/**
	 * Does the actual sell order in the Bitstamp API.
	 * 
	 * @param price
	 * @param amount
	 * @return OpenOrder If the order succeeded, the resulting order
	 * @throws Exception 
	 * @throws JSONException 
	 */
	public OpenOrder placeSellOrder(double price, double amount) throws JSONException, Exception {

		List<NameValuePair> params = new LinkedList<NameValuePair>();
		params.add(new BasicNameValuePair("price", String.valueOf(price)));
		params.add(new BasicNameValuePair("amount", String.valueOf(amount)));
		JSONObject result = new JSONObject(doAuthenticatedCall("https://www.bitstamp.net/api/sell/", params));

		if(result.has("error"))
			throw new Exception(result.get("error").toString());
		
		LOGGER.info("Placed sell order: Sell " + amount + " BTC for " + price + " USD. Result: " + result);
		
		return new OpenOrder(result.getLong("id"), dateStringToSec(result.getString("datetime")), result.getInt("type"), result.getDouble("price"), result.getDouble("amount"));
	}
	
	/**
	 * Does the actual buy order in the Bitstamp API.
	 * 
	 * @param price
	 * @param amount
	 * @return OpenOrder If the order succeeded, the resulting order
	 * @throws Exception 
	 * @throws JSONException 
	 */
	public OpenOrder placeBuyOrder(double price, double amount) throws JSONException, Exception {
		List<NameValuePair> params = new LinkedList<NameValuePair>();
		params.add(new BasicNameValuePair("price", String.valueOf(price)));
		params.add(new BasicNameValuePair("amount", String.valueOf(amount)));
		JSONObject result = new JSONObject(doAuthenticatedCall("https://www.bitstamp.net/api/buy/", params));
		
		if(result.has("error"))
			throw new Exception(result.get("error").toString());
		
		LOGGER.info("Placed buy order: Buy " + amount + " BTC for " + price + " USD. Result: " + result);
		
		return new OpenOrder(result.getLong("id"), dateStringToSec(result.getString("datetime")), result.getInt("type"), result.getDouble("price"), result.getDouble("amount"));
	}

	/**
	 * Does an API call to fetch the currently open orders.
	 * 
	 * @return List of open orders
	 * @throws Exception
	 */
	public List<OpenOrder> getOpenOrders() throws Exception {
		JSONArray result = new JSONArray(doAuthenticatedCall("https://www.bitstamp.net/api/open_orders/"));
		
		List<OpenOrder> openOrders = new LinkedList<OpenOrder>();

		for(int index = 0; index < result.length(); index++) {
			JSONObject row = result.getJSONObject(index);
			openOrders.add(new OpenOrder(row.getLong("id"), dateStringToSec(row.getString("datetime")), row.getInt("type"), row.getDouble("price"), row.getDouble("amount")));
		}
		
		return openOrders;
	}
	
	public long dateStringToSec(String input) throws ParseException {
		return sdf.parse(input).getTime()/1000;
	}

	/**
	 * This method takes a list of open orders and
	 * tries to cancel them one by one.
	 * 
	 * @see getOpenOrders()
	 * 
	 * @param ordersToCancel
	 * @throws JSONException
	 * @throws Exception
	 */
    public void cancelOrders(List<OpenOrder> ordersToCancel) throws JSONException, Exception {
		for(OpenOrder order : ordersToCancel) {
			cancelOrder(order);
		}
	}
    
    public void cancelOrder(OpenOrder order) throws JSONException, Exception {
		List<NameValuePair> params = new LinkedList<NameValuePair>();
		params.add(new BasicNameValuePair("id", String.valueOf(order.getId())));
		String result = doAuthenticatedCall("https://www.bitstamp.net/api/cancel_order/", params);
		LOGGER.info("Cancelled order " + order + ": " + result);
    }
}
