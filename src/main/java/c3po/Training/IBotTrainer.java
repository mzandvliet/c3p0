package c3po.Training;

import c3po.*;
import c3po.simulation.SimulationContext;

public interface IBotTrainer<TBotConfig extends IBotConfig> {
	public TBotConfig train(IBotFactory<TBotConfig> botFactory, SimulationContext simContext);
}
