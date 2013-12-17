package c3po.boundary;

import c3po.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import c3po.macd.MacdAnalysisNode;
import c3po.wallet.IWallet;
import c3po.ISignal;
import c3po.ITradeFloor;
import c3po.ITradeListener;

public class BoundaryBot extends AbstractTickable implements IBot<BoundaryBotConfig> {
	//================================================================================
    // Static Properties
    //================================================================================
	
	private static final Logger LOGGER = LoggerFactory.getLogger(BoundaryBot.class);

	//================================================================================
    // Properties
    //================================================================================
	
	private final int id;
	private final BoundaryBotConfig config;
	private final IWallet wallet;
	private final ITradeFloor tradeFloor;
	
    // Debug references
	
	private final MacdAnalysisNode buyAnalysisNode;
	private final BoundaryTraderNode traderNode;
	

	//================================================================================
    // Methods
    //================================================================================
	
	public BoundaryBot(int id, BoundaryBotConfig config, ISignal price, ISignal volume, IWallet wallet, ITradeFloor tradeFloor) {
		super(config.timestep);
		this.id = id;
		this.config = config;
		this.wallet = wallet;
		this.tradeFloor = tradeFloor;
		
		// Define the signal tree		
		
		buyAnalysisNode = new MacdAnalysisNode(config.timestep, price, volume, config.buyAnalysisConfig);
		
		long startDelayInTicks = config.buyAnalysisConfig.max() / config.timestep;
		traderNode = new BoundaryTraderNode(config.timestep, buyAnalysisNode, wallet, tradeFloor, config.traderConfig, startDelayInTicks);
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
	
	public BoundaryTraderNode getTraderNode() {
		return traderNode;
	}
	
	public BoundaryBotConfig getConfig() {
		return config;
	}

	public String toString() {
		return String.format("Bot ID: %s, Config: [%s]", id, config);
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
