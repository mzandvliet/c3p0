package c3po.macd;

import c3po.*;
import c3po.Training.*;
import c3po.wallet.IWallet;

/* 
 * TODO: Threaded training execution
 * 
 */

public class SelfOptimizingMacdBot extends AbstractTickable implements IBot<SelfOptimizingMacdBotConfig> {
	private final SelfOptimizingMacdBotConfig config;
	private final ISignal price;
	private final ISignal volume;
	private final IWallet wallet;
	private final ITradeFloor tradeFloor;
	
	private final IBotTrainer<MacdBotConfig> trainer;
	
	private MacdBot bot;
	
	private long lastOptimizationTime = -1;
	
	public SelfOptimizingMacdBot(SelfOptimizingMacdBotConfig config, ISignal price, ISignal volume, IWallet wallet, ITradeFloor tradeFloor) {
		super(config.timestep);
		
		this.config = config;
		this.price = price;
		this.volume = volume;
		this.wallet = wallet;
		this.tradeFloor = tradeFloor;
		
		this.trainer = null;
		MacdBotConfig initialConfig = null; // TODO: train on data from *before* the current time, WHICH NEEDS MORE CONFIGURATION
		
		this.bot = new MacdBot(0, initialConfig, price, volume, wallet, tradeFloor);
	}
	
	@Override
	public void addTradeListener(ITradeListener listener) {
		/* 
		 * TODO: 
		 * - register self with internal bot, and present a fully implemented tradeSource interface to the outside world
		 * - When creating new internal bot, you only have to register with the new bot (and don't forget to unregister with the old bot)
		 */
	}

	@Override
	public void removeListener(ITradeListener listener) {
		
	}
	
	@Override
	public SelfOptimizingMacdBotConfig getConfig() {
		return config; // Todo: This is not consistent with bot interface, should probably return hyperconfig that includes trainer & bot config.
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
	public void onNewTick(long tick) {
		// TODO Refactor to support asynchronous optimization, since it's rather costly
		
		if (tick > lastOptimizationTime) {
			MacdBotConfig newConfig = trainer.train(null, null);
			bot = new MacdBot(0, newConfig, price, volume, wallet, tradeFloor); // TODO: are Bots immutable or mutable? Either way, we need to apply the new config to our wrapped bot
		}
		lastOptimizationTime = tick;
		
		bot.onNewTick(tick);
	}

	@Override
	public int getId() {
		// TODO Auto-generated method stub
		return 0;
	}
}
