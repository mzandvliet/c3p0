package c3po.macd;

import c3po.IBotConfig;

public class MacdBotConfig implements IBotConfig {
	public final long timestep; // Does not warrant its own class yet
	
	public final MacdAnalysisConfig buyAnalysisConfig;
	public final MacdAnalysisConfig sellAnalysisConfig;
	public final MacdTraderConfig traderConfig;
	
	public MacdBotConfig(long timeStep, MacdAnalysisConfig buyAnalysisConfig, MacdAnalysisConfig sellAnalysisConfig, MacdTraderConfig traderConfig) {
		this.timestep = timeStep;
		this.buyAnalysisConfig = buyAnalysisConfig;
		this.sellAnalysisConfig = sellAnalysisConfig;
		this.traderConfig = traderConfig;
	}

	@Override
	public String toString() {
		return "Buy: " + buyAnalysisConfig.toString() + ", " + "Sell: " + sellAnalysisConfig.toString() + ", " + traderConfig.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime
				* result
				+ ((buyAnalysisConfig == null) ? 0 : buyAnalysisConfig
						.hashCode());
		result = prime
				* result
				+ ((sellAnalysisConfig == null) ? 0 : sellAnalysisConfig
						.hashCode());
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
		if (buyAnalysisConfig == null) {
			if (other.buyAnalysisConfig != null)
				return false;
		} else if (!buyAnalysisConfig.equals(other.buyAnalysisConfig))
			return false;
		if (sellAnalysisConfig == null) {
			if (other.sellAnalysisConfig != null)
				return false;
		} else if (!sellAnalysisConfig.equals(other.sellAnalysisConfig))
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
