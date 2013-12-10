package c3po.macd;

import c3po.*;

import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.util.Date;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import c3po.bitstamp.BitstampSimulationTradeFloor;
import c3po.bitstamp.BitstampSimulationTickerDbSource;
import c3po.DbConnection;
import c3po.IClock;
import c3po.ISignal;
import c3po.ITradeFloor;
import c3po.ITradeListener;
import c3po.IWallet;
import c3po.Time;

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
	private final MacdTraderNode traderNode;
	

	//================================================================================
    // Methods
    //================================================================================
	
	public MacdBot(int id, MacdBotConfig config, ISignal ticker, IWallet wallet, ITradeFloor tradeFloor) {
		super(config.timestep);
		this.id = id;
		this.config = config;
		this.wallet = wallet;
		this.tradeFloor = tradeFloor;
		
		// Define the signal tree		
		
		buyAnalysisNode = new MacdAnalysisNode(config.timestep, ticker, config.buyAnalysisConfig);
		sellAnalysisNode = new MacdAnalysisNode(config.timestep, ticker, config.sellAnalysisConfig);
		
		long startDelayInTicks = Math.max(config.buyAnalysisConfig.max() / config.timestep, config.sellAnalysisConfig.max() / config.timestep);
		traderNode = new MacdTraderNode(config.timestep, buyAnalysisNode.getOutputDifference(), sellAnalysisNode.getOutputDifference(),  wallet, tradeFloor, config.traderConfig, startDelayInTicks);
	}

	@Override
	public void onNewTick(long tick) {
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
