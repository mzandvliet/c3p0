package c3po;

import c3po.wallet.IWallet;

public interface IBot<TBotConfig extends IBotConfig> extends ITickable, ITradeActionSource {
	public TBotConfig getConfig();
	public IWallet getWallet();
	public ITradeFloor getTradeFloor();
	public int getId();
}