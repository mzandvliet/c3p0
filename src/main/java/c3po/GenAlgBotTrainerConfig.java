package c3po;

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
}
