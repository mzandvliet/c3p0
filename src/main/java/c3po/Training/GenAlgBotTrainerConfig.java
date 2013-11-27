package c3po.Training;

public class GenAlgBotTrainerConfig {
	// Simulation and fitness test
	public final int numEpochs;
	public final int numBots;
	
	// Selection
	public final int numParents;
	public final int numElites;
	
	public GenAlgBotTrainerConfig(int numEpochs, int numBots, int numParents,
			int numElites, double mutationChance) {
		
		this.numEpochs = numEpochs;
		this.numBots = numBots;
		this.numParents = numParents;
		this.numElites = numElites;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + numBots;
		result = prime * result + numElites;
		result = prime * result + numEpochs;
		result = prime * result + numParents;
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
		GenAlgBotTrainerConfig other = (GenAlgBotTrainerConfig) obj;
		if (numBots != other.numBots)
			return false;
		if (numElites != other.numElites)
			return false;
		if (numEpochs != other.numEpochs)
			return false;
		if (numParents != other.numParents)
			return false;
		return true;
	}
}
