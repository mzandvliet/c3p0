package c3po.boundary;

import java.util.Random;

import c3po.Training.*;
import c3po.simulation.SimulationContext;

/*
 * TODO:
 * 
 * Replace this with factory methods in bot implementations, taking an instance of their specific BotConfig implementation
 */

public class BoundaryBotFactory implements IBotFactory<BoundaryBotConfig> {

	private final SimulationContext context;
	
	public BoundaryBotFactory(SimulationContext context) {
		super();
		this.context = context;
	}

	@Override
	public BoundaryBot create(BoundaryBotConfig config) {
		return new BoundaryBot(new Random().nextInt(), config, context.getPriceSignal(), context.getVolumeSignal(), context.getWalletInstance(), context.getTradeFloor());
	}
}
