package c3po;

public interface IBot<TBotConfig extends IBotConfig> extends ITickable, ITradeActionSource {
	public TBotConfig getConfig();
	public IWallet getWallet();
	public ITradeFloor getTradeFloor();
}