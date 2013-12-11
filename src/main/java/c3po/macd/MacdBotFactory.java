package c3po.macd;

import java.util.Random;

import c3po.*;
import c3po.Training.*;
import c3po.simulation.SimulationContext;

/*
 * TODO:
 * 
 * Replace this with factory methods in bot implementations, taking an instance of their specific BotConfig implementation
 */

public class MacdBotFactory implements IBotFactory<MacdBotConfig> {

	private final SimulationContext context;
	
	public MacdBotFactory(SimulationContext context) {
		super();
		this.context = context;
	}

	@Override
	public MacdBot create(MacdBotConfig config) {
		return new MacdBot(new Random().nextInt(), config, context.getSignal(), context.getWalletInstance(), context.getTradeFloor());
	}
}
