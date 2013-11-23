package c3po;

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
}
