package c3po.macd;

import c3po.*;

public class SelfOptimizingMacdBot extends AbstractTickable implements IBot {
	
	private MacdBot bot;
	
	public SelfOptimizingMacdBot(long deltatime, ISignal ticker, IWallet wallet, ITradeFloor tradeFloor) {
		super(deltatime);
		
		MacdBotTrainer trainer = new MacdBotTrainer();
		bot = new MacdBot(null, null, null, null);
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
	public IWallet getWallet() {
		return bot.getWallet();
	}

	@Override
	public ITradeFloor getTradeFloor() {
		return bot.getTradeFloor();
	}

	@Override
	public void onNewTick(long tick) {
		
		// Todo: Run genalg every one in a while, update existing bot with new config
		
		bot.onNewTick(tick);
	}
}
