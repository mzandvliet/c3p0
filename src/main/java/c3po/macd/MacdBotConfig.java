package c3po.macd;

public class MacdBotConfig {
	public final long timeStep; // Does not warrant its own class yet
	public final MacdAnalysisConfig analysisConfig;
	public final MacdTraderConfig traderConfig;
	
	public MacdBotConfig(long timeStep, MacdAnalysisConfig analysisConfig, MacdTraderConfig traderConfig) {
		this.timeStep = timeStep;
		this.analysisConfig = analysisConfig;
		this.traderConfig = traderConfig;
	}
}
