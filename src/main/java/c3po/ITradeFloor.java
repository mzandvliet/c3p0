package c3po;

import java.util.List;

/* Todo:
 * 
 * - This is a really poor and impromptu interface, but it demonstrates the principle
 * 
 * - Don't do action logging within the trade. Instead, it should fire TradeAction events and something else should record them
 */

public interface ITradeFloor {
	public double getWalledUsd();
	public double getWalletBtc();
	
	public double toBtc(double usd);
	public double toUsd(double btc);
	
	public double buy(long timestamp, double volume);
	public double sell(long timestamp, double volume);
	
	public List<TradeAction> getActions();
	double getWalletValue();
	
	public void dump();
}
