package c3po.simulation;

import c3po.*;

public class SimulationContext {
	private final INonRealtimeSource source;
	private final ISimulationClock clock;
	private final ISignal signal;
	private final ITradeFloor tradeFloor;
	private final IWallet wallet;
	
	private long startTime;
	private long endTime;
	
	public SimulationContext(
			INonRealtimeSource source,
			ISimulationClock clock,
			ISignal signal,
			ITradeFloor tradeFloor,
			IWallet wallet
			) {
		super();
		this.source = source;
		this.clock = clock;
		this.signal = signal;
		this.tradeFloor = tradeFloor;
		this.wallet = wallet;
	}

	public INonRealtimeSource getSource() {
		return source;
	}

	public ISignal getSignal() {
		return signal;
	}

	public ITradeFloor getTradeFloor() {
		return tradeFloor;
	}

	public IWallet getWalletInstance() {
		return wallet.copy();
	}

	public ISimulationClock getClock() {
		return clock;
	}
	
	public void initializeForTimePeriod(long startTime, long endTime) {
		this.startTime = startTime;
		this.endTime = endTime;
		source.initializeForTimePeriod(startTime, endTime);
	}
	
	public void run() {
		clock.run(startTime, endTime);
	}
	
	public void reset() {
		source.reset();
	}
}
