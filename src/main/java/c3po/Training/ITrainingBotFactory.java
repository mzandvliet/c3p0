package c3po.Training;

import c3po.IBot;
import c3po.IBotConfig;

public interface ITrainingBotFactory<TBotConfig extends IBotConfig> {
	public IBot<TBotConfig> create(TBotConfig config);
}
