package c3po;

import c3po.structs.TradeIntention;

public interface ITradeListener {
	public void onTrade(TradeIntention action);
}
