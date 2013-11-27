package c3po.Training;

import c3po.IBotConfig;

public interface IBotConfigMutator<TBotConfig extends IBotConfig>{
	public TBotConfig createRandomConfig();
	public TBotConfig crossBreedConfig(final TBotConfig parentA, final TBotConfig parentB);
	public TBotConfig mutateConfig(final TBotConfig config);
	public TBotConfig validateConfig(final TBotConfig config);
}
