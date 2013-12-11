package c3po;

public interface ISimulationClock extends IClock {
	public void run(long startTime, long endTime);
}
