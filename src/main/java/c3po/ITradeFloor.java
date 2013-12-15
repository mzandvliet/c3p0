package c3po;

import c3po.wallet.IWallet;

/* Todo:
 * 
 * - This is a really poor and impromptu interface, but it demonstrates the principle
 * 
 * - Don't do action logging within the trade. Instead, it should fire TradeAction events and something else should record them
 */

public interface ITradeFloor extends ITradeActionSource {
	public double toBtc(double usd);
	public double toUsd(double btc);

	public double buy(IWallet wallet, TradeAction action);
	public double sell(IWallet wallet, TradeAction action);
	
	double getWalletValueInUsd(IWallet wallet);
	void updateWallet(IWallet wallet);
	
	public double peekBid() throws Exception;
	
	/**
	 * This method readjusts open orders to match the current prices
	 * in an attempt to fill them as soon as possible.
	 */
	public void adjustOrders();
	
	/**
	 * @return True if the orders need to be set as limit orders, instead of instant fulfillment.
	 *         Gives potentially a greater margin if there are enough trades, but might be wrong
	 *         in a market without sell or buy orders.
	 */
	public boolean doLimitOrder();
}
