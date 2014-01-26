package c3po.macd;

import c3po.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import c3po.wallet.IWallet;
import c3po.IOverrideModusChecker;
import c3po.ISignal;
import c3po.ITradeFloor;
import c3po.ITradeListener;

public class MacdBot extends AbstractTickable implements IBot<MacdBotConfig> {
	//================================================================================
    // Static Properties
    //================================================================================
	
	private static final Logger LOGGER = LoggerFactory.getLogger(MacdBot.class);

	//================================================================================
    // Properties
    //================================================================================
	
	private final int id;
	private final MacdBotConfig config;
	private final IWallet wallet;
	private final ITradeFloor tradeFloor;
	
    // Debug references
	
	private final MacdAnalysisNode buyAnalysisNode;
	private final MacdAnalysisNode sellAnalysisNode;
	private final MacdAnalysisNode volumeAnalysisNode;
	private final MacdTraderNode traderNode;
	

	//================================================================================
    // Methods
    //================================================================================
	
	public MacdBot(int id, MacdBotConfig config, ISignal price, ISignal volume, IWallet wallet, ITradeFloor tradeFloor) {
		super(config.timestep);
		this.id = id;
		this.config = config;
		this.wallet = wallet;
		this.tradeFloor = tradeFloor;
		
		// Define the signal tree		
		
		buyAnalysisNode = new MacdAnalysisNode(config.timestep, price, config.buyAnalysisConfig);
		sellAnalysisNode = new MacdAnalysisNode(config.timestep, price, config.sellAnalysisConfig);
		volumeAnalysisNode = new MacdAnalysisNode(config.timestep, volume, config.volumeAnalysisConfig);
		
		long startDelayInTicks = Math.max(config.buyAnalysisConfig.max() / config.timestep, config.sellAnalysisConfig.max() / config.timestep);
		traderNode = new MacdTraderNode(config.timestep, price, buyAnalysisNode, sellAnalysisNode, volumeAnalysisNode, wallet, tradeFloor, config.traderConfig, startDelayInTicks);
	}

	@Override
	public void update(long tick) {
		tradeFloor.updateWallet(wallet);
		tradeFloor.adjustOrders();
		traderNode.tick(tick);
	}
	
	@Override
	public long getTimestep() {
		return config.timestep;
	}

	@Override
	public IWallet getWallet() {
		return wallet;
	}

	@Override
	public ITradeFloor getTradeFloor() {
		return tradeFloor;
	}
	
	@Override
	public int getId() {
		return id;
	}

	public MacdAnalysisNode getBuyAnalysisNode() {
		return buyAnalysisNode;
	}
	
	public MacdAnalysisNode getSellAnalysisNode() {
		return sellAnalysisNode;
	}
	
	public MacdTraderNode getTraderNode() {
		return traderNode;
	}
	
	public MacdBotConfig getConfig() {
		return config;
	}

	public String toString() {
		return String.format("Bot ID: %s, Config: [%s]", id, config);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((config == null) ? 0 : config.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MacdBot other = (MacdBot) obj;
		if (config == null) {
			if (other.config != null)
				return false;
		} else if (!config.equals(other.config))
			return false;
		return true;
	}

	@Override
	public void addTradeListener(ITradeListener listener) {
		traderNode.addTradeListener(listener);
	}

	@Override
	public void removeListener(ITradeListener listener) {
		traderNode.removeListener(listener);
	}
}
