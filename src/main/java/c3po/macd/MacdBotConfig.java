package c3po.macd;

import c3po.IBotConfig;

public class MacdBotConfig implements IBotConfig {
	public final long timestep; // Does not warrant its own class yet
	public final MacdAnalysisConfig analysisConfig;
	public final MacdTraderConfig traderConfig;
	
	public MacdBotConfig(long timeStep, MacdAnalysisConfig analysisConfig, MacdTraderConfig traderConfig) {
		this.timestep = timeStep;
		this.analysisConfig = analysisConfig;
		this.traderConfig = traderConfig;
	}

	@Override
	public String toString() {
		return analysisConfig.toString() + ", " + traderConfig.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((analysisConfig == null) ? 0 : analysisConfig.hashCode());
		result = prime * result + (int) (timestep ^ (timestep >>> 32));
		result = prime * result
				+ ((traderConfig == null) ? 0 : traderConfig.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MacdBotConfig other = (MacdBotConfig) obj;
		if (analysisConfig == null) {
			if (other.analysisConfig != null)
				return false;
		} else if (!analysisConfig.equals(other.analysisConfig))
			return false;
		if (timestep != other.timestep)
			return false;
		if (traderConfig == null) {
			if (other.traderConfig != null)
				return false;
		} else if (!traderConfig.equals(other.traderConfig))
			return false;
		return true;
	}
}
