package c3po.macd;

import c3po.IBotConfig;
import c3po.Training.GenAlgBotTrainerConfig;

public class SelfOptimizingMacdBotConfig implements IBotConfig {
	public final long timestep;
	public final long optimizationTimestep;
	public final GenAlgBotTrainerConfig genAlgConfig;
	public final MacdBotMutatorConfig mutatorConfig;
	
	public SelfOptimizingMacdBotConfig(
			long timestep,
			long optimizationTimestep,
			GenAlgBotTrainerConfig genAlgConfig,
			MacdBotMutatorConfig mutatorConfig) {
		
		this.timestep = timestep;
		this.optimizationTimestep = optimizationTimestep;
		this.genAlgConfig = genAlgConfig;
		this.mutatorConfig = mutatorConfig;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((genAlgConfig == null) ? 0 : genAlgConfig.hashCode());
		result = prime * result
				+ ((mutatorConfig == null) ? 0 : mutatorConfig.hashCode());
		result = prime * result
				+ (int) (optimizationTimestep ^ (optimizationTimestep >>> 32));
		result = prime * result + (int) (timestep ^ (timestep >>> 32));
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
		SelfOptimizingMacdBotConfig other = (SelfOptimizingMacdBotConfig) obj;
		if (genAlgConfig == null) {
			if (other.genAlgConfig != null)
				return false;
		} else if (!genAlgConfig.equals(other.genAlgConfig))
			return false;
		if (mutatorConfig == null) {
			if (other.mutatorConfig != null)
				return false;
		} else if (!mutatorConfig.equals(other.mutatorConfig))
			return false;
		if (optimizationTimestep != other.optimizationTimestep)
			return false;
		if (timestep != other.timestep)
			return false;
		return true;
	}
}
