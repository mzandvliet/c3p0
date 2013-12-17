package c3po.boundary;

import com.google.gson.Gson;

import c3po.IBotConfig;
import c3po.macd.MacdAnalysisConfig;
import c3po.macd.MacdTraderConfig;

public class BoundaryBotConfig implements IBotConfig {
	public final long timestep;
	
	public final MacdAnalysisConfig buyAnalysisConfig;
	public final BoundaryTraderConfig traderConfig;
	
	public BoundaryBotConfig(long timeStep, MacdAnalysisConfig buyAnalysisConfig, BoundaryTraderConfig traderConfig) {
		this.timestep = timeStep;
		this.buyAnalysisConfig = buyAnalysisConfig;
		this.traderConfig = traderConfig;
	}
	
	/**
	 * This method takes the configuration in JSON form
	 * and outputs a new Config object.
	 * 
	 * @param json
	 * @return Config object
	 */
	public static BoundaryBotConfig fromJSON(String json) {
		Gson gson = new Gson();
		return gson.fromJson(json, BoundaryBotConfig.class);
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
		//return toJSON();
		return "Buy: " + buyAnalysisConfig.toString() + ", " + traderConfig.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime
				* result
				+ ((buyAnalysisConfig == null) ? 0 : buyAnalysisConfig
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
		BoundaryBotConfig other = (BoundaryBotConfig) obj;
		if (buyAnalysisConfig == null) {
			if (other.buyAnalysisConfig != null)
				return false;
		} else if (!buyAnalysisConfig.equals(other.buyAnalysisConfig))
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
