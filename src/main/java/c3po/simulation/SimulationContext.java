package c3po.simulation;

import c3po.*;

public class SimulationContext {
	private final INonRealtimeSource source;
	private final IClock clock;
	
	public SimulationContext(INonRealtimeSource source, ISignal tickerSignal,
			IClock clock) {
		super();
		this.source = source;
		this.clock = clock;
	}

	public IClock getClock() {
		return clock;
	}

	public void reset() {
		source.reset();
	}
}
