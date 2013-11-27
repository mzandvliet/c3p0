package c3po.macd;

import c3po.*;
import c3po.Training.*;

public class SelfOptimizingMacdBot extends AbstractTickable implements IBot<MacdBotConfig> {
	private final IBotTrainer<MacdBotConfig> trainer;
	private final MacdBot bot;
	
	public SelfOptimizingMacdBot(long timestep,
			IBotTrainer<MacdBotConfig> trainer,
			MacdBot bot) {
		super(timestep);
		
		this.bot = bot;
		this.trainer = trainer;
	}

	@Override
	public void addTradeListener(ITradeListener listener) {
		bot.addTradeListener(listener);
	}

	@Override
	public void removeListener(ITradeListener listener) {
		bot.removeListener(listener);
	}
	
	@Override
	public MacdBotConfig getConfig() {
		return bot.getConfig(); // Todo: This is not consistent with bot interface, should probably return hyperconfig that includes trainer & bot config.
	}

	@Override
	public IWallet getWallet() {
		return bot.getWallet();
	}

	@Override
	public ITradeFloor getTradeFloor() {
		return bot.getTradeFloor();
	}

	@Override
	public void onNewTick(long tick) {
		
		// Todo: Run genalg every one in a while, AND REMEMBER TO SUPPORT NON-BLOCKING CALCULATION
		
		MacdBotConfig newConfig = trainer.train();
		bot = new MacdBot(newConfig, , null, null); // Todo: are Bots immutable or mutable? Either way, we need to apply the new config to our wrapped bot
		
		bot.onNewTick(tick);
	}
}
