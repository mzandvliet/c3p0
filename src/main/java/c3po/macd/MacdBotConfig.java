package c3po.macd;

import com.google.gson.Gson;

import c3po.IBotConfig;

public class MacdBotConfig implements IBotConfig {
	public final long timestep;
	
	public final MacdAnalysisConfig buyAnalysisConfig;
	public final MacdAnalysisConfig sellAnalysisConfig;
	public final MacdAnalysisConfig volumeAnalysisConfig;
	public final MacdTraderConfig traderConfig;
	
	public MacdBotConfig(long timeStep, MacdAnalysisConfig buyAnalysisConfig, MacdAnalysisConfig sellAnalysisConfig, MacdAnalysisConfig volumeAnalysisConfig, MacdTraderConfig traderConfig) {
		this.timestep = timeStep;
		this.buyAnalysisConfig = buyAnalysisConfig;
		this.sellAnalysisConfig = sellAnalysisConfig;
		this.volumeAnalysisConfig = volumeAnalysisConfig;
		this.traderConfig = traderConfig;
	}
	
	/**
	 * This method takes the configuration in JSON form
	 * and outputs a new Config object.
	 * 
	 * @param json
	 * @return Config object
	 */
	public static MacdBotConfig fromJSON(String json) {
		Gson gson = new Gson();
		return gson.fromJson(json, MacdBotConfig.class);
	}
	
	/**
	 * Outputs the Config as a JSON file
	 * @return
	 */
	public String toJSON() {
		Gson gson = new Gson();
		return gson.toJson(this);
	}

	@Override
	public String toString() {
		return "Buy: " + buyAnalysisConfig.toString() + ", Sell: " + sellAnalysisConfig.toString() + ", Volume" + volumeAnalysisConfig.toString() + ", " + traderConfig.toString();
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
		result = prime
				* result
				+ ((volumeAnalysisConfig == null) ? 0 : volumeAnalysisConfig
						.hashCode());
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
		if (volumeAnalysisConfig == null) {
			if (other.volumeAnalysisConfig != null)
				return false;
		} else if (!volumeAnalysisConfig.equals(other.volumeAnalysisConfig))
			return false;
		return true;
	}
}
