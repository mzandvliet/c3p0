package c3po.Training;

import c3po.IBot;
import c3po.IBotConfig;

public interface IBotFactory<TBotConfig extends IBotConfig> {
	public IBot<TBotConfig> create(TBotConfig config);
}
