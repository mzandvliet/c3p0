package c3po.Training;

import c3po.*;

public interface IBotTrainer<TBotConfig extends IBotConfig> {
	public TBotConfig train();
}
