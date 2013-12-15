package c3po.clock;


public interface ISimulationClock extends IClock {
	public void run(long startTime, long endTime);
}
