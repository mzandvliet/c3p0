package c3po.Training;

import c3po.IBot;
import c3po.IBotConfig;

public interface ITrainingBotFactory<TBotConfig extends IBotConfig, TBot extends IBot<TBotConfig>> {
	public TBot create(TBotConfig config);
}
