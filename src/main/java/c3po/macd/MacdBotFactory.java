package c3po.macd;

import java.util.Random;

import c3po.*;
import c3po.Training.*;
import c3po.simulation.SimulationContext;

/*
 * Todo:
 * 
 * This wouldn't be necessary if you could create bots without supplying ticker, tradefloor, wallet, etc.
 * AND if you could create instances of generic types, which you can't.
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
