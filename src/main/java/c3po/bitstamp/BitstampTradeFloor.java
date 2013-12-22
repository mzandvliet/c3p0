package c3po.bitstamp;

import java.io.IOException;
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
import c3po.TradeAction;
import c3po.structs.OpenOrder;
import c3po.utils.JsonReader;
import c3po.utils.Time;
import c3po.wallet.IWallet;


public class BitstampTradeFloor extends AbstractTradeFloor {
	private static final Logger LOGGER = LoggerFactory.getLogger(BitstampTradeFloor.class);
	private double tradeFee = 0.0022d;
	
	/**
	 * Because of tradelag and such its handy to undercut a bit, to avoid missing the boat
	 * and not having your orders filled.
	 */
	private static double priceUndercut = 0.05d;
	
	// Default: Dagobert
	private static int clientId = 821581;
	private static String apiKey = "rOlWvTyTL0ZZL7OztIH9nCbS66WOEc4h";
	private static String apiSecret = "NksKaAEE9ZbcMTu1nJ1YiOAbdJT0lqTh";
	
	SimpleDateFormat sdf  = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");
	
	private long lastWalletUpdate;
	private long lastAdjustOrdersUpdate;
	
	public BitstampTradeFloor(ISignal last, ISignal bid, ISignal ask, boolean doLimitOrder) {
		super(last, bid, ask, doLimitOrder);
		
		// Real stuff is happening, extra JSON logging
		JsonReader.debug = true;
	}
	
	public BitstampTradeFloor(ISignal last, ISignal bid, ISignal ask, boolean doLimitOrder, int clientId, String apiKey, String apiSecret) {
		this(last, bid, ask, doLimitOrder);
		BitstampTradeFloor.clientId = clientId;
		BitstampTradeFloor.apiKey = apiKey;
		BitstampTradeFloor.apiSecret = apiSecret;
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
	
	/**
	 * Does a call to the Bitstamp Ticker for the most up to date Bid available
	 * 
	 * @return Most up to date bid ever
	 * @throws JSONException
	 * @throws IOException
	 */
	private double getCurrentBid() throws JSONException, IOException {
		JSONObject json = JsonReader.readJsonFromUrl("http://www.bitstamp.net/api/ticker/");
		return json.getDouble("bid");
	}
	
	/**
	 * Does a call to the Bitstamp Ticker for the most up to date Ask available
	 * 
	 * @return Most up to date ask ever
	 * @throws JSONException
	 * @throws IOException
	 */
	private double getCurrentAsk() throws JSONException, IOException {
		JSONObject json = JsonReader.readJsonFromUrl("http://www.bitstamp.net/api/ticker/");
		return json.getDouble("ask");
	}

	/**
	 * Transforms a double to string, with a maximum of 6 digits
	 * 
	 * @param input
	 * @return String with maximum of 6 digits
	 */
	public static String doubleToPriceString(double input) {		
		return String.valueOf(Math.floor(input * 100.0d) / 100.d);
	}
	
	public static String doubleToAmountString(double input) {		
		return String.valueOf(Math.floor(input * 100000000.0d) / 100000000.d);
	}
	
	public String doAuthenticatedCall(String url, List<NameValuePair> params) throws Exception {	
		long nonce = generateNonce();
		String sig = generateSignature(nonce);
		
		params.add(new BasicNameValuePair("key", apiKey));
		params.add(new BasicNameValuePair("nonce", String.valueOf(nonce)));
		params.add(new BasicNameValuePair("signature", sig));
		return JsonReader.readJsonFromUrl(url, params);
	}
	
	/**
	 * This method decides which price to use for the new order.
	 * Currently it checks if it must do a limit order, in which
	 * case he raises the current bid with a dollarcent. If we want
	 * to use an sell order to make it instant, we use the current ask instead. 
	 * 
	 * @uses doLimitOrder
	 * @return
	 * @throws JSONException
	 * @throws IOException
	 */
	protected double getBuyPrice() throws JSONException, IOException {
		if(doLimitOrder) {
			return this.getCurrentBid() + priceUndercut;
		} else {
			return this.getCurrentAsk() + priceUndercut;
		}
	}
	
	/**
	 * This method decides which price to use for the new order.
	 * Currently it checks if it must do a limit order, in which
	 * case he undercuts the current ask with a dollarcent. If we want
	 * to use a buy order to make it instant, we use the current bid instead. 
	 * 
	 * @uses doLimitOrder
	 * @return
	 * @throws JSONException
	 * @throws IOException
	 */
	protected double getSellPrice() throws JSONException, IOException {
		if(doLimitOrder) {
			return this.getCurrentAsk() - priceUndercut;
		} else {
			return this.getCurrentBid() - priceUndercut;
		}
	}

	@Override
	public OpenOrder buyImpl(IWallet wallet, TradeAction action) {
		OpenOrder order = null;
		try {
			// The amount of Btc we are going to get if we buy for volume USD
			double buyPrice = getBuyPrice();
			double boughtBtc = calculateBtcToBuy(action.volume, buyPrice, tradeFee);
			
			// Place the actual buy order
			order = placeBuyOrder(buyPrice, boughtBtc);
			
			// We reserve the dollars that we need for this transaction
			wallet.reserve(action.volume, 0);
		}
		catch(Exception e) {
			LOGGER.error("Could not buy BTC", e);
		}
		
		return order;
	}
	
	/**
	 * Calculated the amount of BTC that you can buy given the volume, the current price
	 * and the tradefee percentage.
	 * 
	 * @param volumeUsd
	 * @param buyPrice
	 * @param tradeFee
	 * @return Amount of BTC
	 */
	public static double calculateBtcToBuy(double volumeUsd, double buyPrice, double tradeFee) {
		return (volumeUsd - calculateTradeFeeUsd(volumeUsd, tradeFee)) / buyPrice;
	}
	
	/**
	 * This method calculated the tradefee in USD based on the volume USD and the current tradefee
	 * percentage. The amount is always rounded up as of Bitstamps regulations.
	 * 
	 * @param volumeUsd
	 * @param tradeFee
	 * @return Rounded up tradefee in USD
	 */
	public static double calculateTradeFeeUsd(double volumeUsd, double tradeFee) {
		double expectedTradefee = Math.ceil(volumeUsd * tradeFee * 100) / 100;
		LOGGER.debug("Expected tradeFee: " + BitstampTradeFloor.doubleToPriceString(expectedTradefee));
		return expectedTradefee;
	}

	@Override
	public OpenOrder sellImpl(IWallet wallet, TradeAction action) {
		OpenOrder order = null;
		try {
			// We get the latest ask, assuming the ticker is updated by some other part of the app
			double sellPrice = this.getSellPrice();

			double soldBtc = action.volume;
			
			// Place the actual sell order
			order = placeSellOrder(sellPrice, soldBtc);
			
			// Reserving the btc's so they dont get spend twice
			wallet.reserve(0, action.volume);
		}
		catch(Exception e) {
			LOGGER.error("Could not sell BTC", e);
		}
		
		return order;
	}
	
	@Override
	public void updateWallet(IWallet wallet) {
		
		if(lastWalletUpdate + 1 * Time.MINUTES < new Date().getTime()) {
			lastWalletUpdate = new Date().getTime();
			
			try {
				JSONObject result = new JSONObject(doAuthenticatedCall("https://www.bitstamp.net/api/balance/"));
				wallet.update(lastWalletUpdate, result.getDouble("usd_available"), result.getDouble("btc_available"), result.getDouble("usd_reserved"), result.getDouble("btc_reserved"));
				
				// Update tradeFee if needed
				double newFee = result.getDouble("fee");
				setTradeFeeInPercent(newFee);
			} catch (Exception e) {
				LOGGER.error("Could not update wallet", e);
			}
		}
	}
	
	private void setTradeFeeInPercent(double tradeFeePercent) {
		double newFee = tradeFeePercent / 100d;
		if(newFee != tradeFee) {
			LOGGER.info("Updated tradeFee - Old: " + tradeFee + " New: " + newFee);
			tradeFee = newFee;
		}
	}

	
	/**
	 * This method looks at the currently open orders of the bot
	 * and readjusts them if the prices are outdated. This makes
	 * sure open orders are filled as soon as possible.
	 */
	@Override
	public void adjustOrders() {
		if(lastAdjustOrdersUpdate + 1 * Time.MINUTES < new Date().getTime()) {
			lastAdjustOrdersUpdate = new Date().getTime();
			
			try {
				List<OpenOrder> openOrders = getOpenOrders();
				
				// Stop in case of no open orders
				if(openOrders.size() == 0)
					return;
				
				// Decide which would be the ideal price to buy or sell for
				double buyPrice = getBuyPrice();
				double sellPrice = getSellPrice();
			
				// Loop over all the open orders
				for(OpenOrder openOrder : openOrders) {
					// Adjust sell order if needed
					if(openOrder.getType() == OpenOrder.SELL && openOrder.getPrice() != sellPrice) {
						LOGGER.info("Adjusting "+ openOrder + " to match price " + sellPrice);
						cancelOrder(openOrder);
						
						// We need to place a new order for the same amount of Btc
						placeSellOrder(sellPrice, openOrder.getAmount());
					}
					
					// Adjust buy order if needed
					if(openOrder.getType() == OpenOrder.BUY && openOrder.getPrice() != buyPrice) {
						LOGGER.info("Adjusting "+ openOrder + " to match price " + buyPrice);
						cancelOrder(openOrder);
						
						// Modify the amount of Btc's to buy with the current price, so we spend the same amount of dollars
						double orderValue = openOrder.getPrice() * openOrder.getAmount();
						
						placeBuyOrder(buyPrice, orderValue / buyPrice);
					}
				}
				
			} catch(Exception e) {
				LOGGER.error("Could not adjust orders", e);
			}
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
		params.add(new BasicNameValuePair("price", String.valueOf(doubleToPriceString(price))));
		params.add(new BasicNameValuePair("amount", String.valueOf(doubleToAmountString(amount))));
		JSONObject result = new JSONObject(doAuthenticatedCall("https://www.bitstamp.net/api/sell/", params));

		if(result.has("error"))
			throw new Exception(result.get("error").toString());
		
		LOGGER.info("Placed sell order: Sell " + doubleToAmountString(amount) + " BTC for " + doubleToPriceString(price) + " USD. Result: " + result);
		
		return new OpenOrder(result.getLong("id"), dateStringToSec(result.getString("datetime")), result.getInt("type"), result.getDouble("price"), result.getDouble("amount"));
	}
	
	/**
	 * Does the actual buy order in the Bitstamp API.
	 * 
	 * @param price
	 * @param btcToBuy
	 * @return OpenOrder If the order succeeded, the resulting order
	 * @throws Exception 
	 * @throws JSONException 
	 */
	public OpenOrder placeBuyOrder(double price, double btcToBuy) throws JSONException, Exception {
		List<NameValuePair> params = new LinkedList<NameValuePair>();
		params.add(new BasicNameValuePair("price", String.valueOf(doubleToPriceString(price))));
		params.add(new BasicNameValuePair("amount", String.valueOf(doubleToAmountString(btcToBuy))));
		JSONObject result = new JSONObject(doAuthenticatedCall("https://www.bitstamp.net/api/buy/", params));
		
		if(result.has("error"))
			throw new Exception(result.get("error").toString());
		
		LOGGER.info("Placed buy order: Buy " + doubleToAmountString(btcToBuy) + " BTC for " + doubleToPriceString(price) + " USD. Result: " + result);
		
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

	@Override
	public double peekBid() throws Exception {
		return this.getCurrentBid();
	}
}
