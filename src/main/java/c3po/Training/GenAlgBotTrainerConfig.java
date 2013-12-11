package c3po.Training;

public class GenAlgBotTrainerConfig {
	// Training set
	public final long dataStartTime;
	public final long dataEndTime;
	
	// Simulation
	public final long simulationLength;
	public final int numEpochs;
	public final int numSimulationsPerEpoch;
	
	// Selection
	public final int numBots;
	public final int numParents;
	public final int numElites;
	
	public GenAlgBotTrainerConfig(long dataStartTime, long dataEndTime,
			long simulationLength, int numEpochs, int numSimulationsPerEpoch,
			int numBots, int numParents, int numElites) {
		super();
		this.dataStartTime = dataStartTime;
		this.dataEndTime = dataEndTime;
		this.simulationLength = simulationLength;
		this.numEpochs = numEpochs;
		this.numSimulationsPerEpoch = numSimulationsPerEpoch;
		this.numBots = numBots;
		this.numParents = numParents;
		this.numElites = numElites;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (dataEndTime ^ (dataEndTime >>> 32));
		result = prime * result
				+ (int) (dataStartTime ^ (dataStartTime >>> 32));
		result = prime * result + numBots;
		result = prime * result + numElites;
		result = prime * result + numEpochs;
		result = prime * result + numParents;
		result = prime * result + numSimulationsPerEpoch;
		result = prime * result
				+ (int) (simulationLength ^ (simulationLength >>> 32));
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
		if (dataEndTime != other.dataEndTime)
			return false;
		if (dataStartTime != other.dataStartTime)
			return false;
		if (numBots != other.numBots)
			return false;
		if (numElites != other.numElites)
			return false;
		if (numEpochs != other.numEpochs)
			return false;
		if (numParents != other.numParents)
			return false;
		if (numSimulationsPerEpoch != other.numSimulationsPerEpoch)
			return false;
		if (simulationLength != other.simulationLength)
			return false;
		return true;
	}
}
