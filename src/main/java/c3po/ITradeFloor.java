package c3po;

import java.util.List;

import c3po.TradeAction.TradeActionType;

/* Todo:
 * 
 * - This is a really poor and impromptu interface, but it demonstrates the principle
 * 
 * - Don't do action logging within the trade. Instead, it should fire TradeAction events and something else should record them
 */

public interface ITradeFloor extends ITradeActionSource {
	public double getWalledUsd();
	public double getWalletBtc();
	
	public double toBtc(double usd);
	public double toUsd(double btc);

	public double buy(TradeAction action);
	public double sell(TradeAction action);
	
	double getWalletValue();
}
