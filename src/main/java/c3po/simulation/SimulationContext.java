package c3po.simulation;

import c3po.*;

public class SimulationContext {
	private final INonRealtimeSource source;
	private final ISignal signal;
	private final ITradeFloor tradeFloor;
	private final IWallet wallet;
	private final SimulationClock clock;
	
	public SimulationContext(
			INonRealtimeSource source,
			ISignal signal,
			ITradeFloor tradeFloor,
			IWallet wallet,
			SimulationClock clock) {
		super();
		this.source = source;
		this.signal = signal;
		this.tradeFloor = tradeFloor;
		this.wallet = wallet;
		this.clock = clock;
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

	public SimulationClock getClock() {
		return clock;
	}
	
	public void reset() {
		source.reset();
	}
	
	public void initializeForTimePeriod(long startTime, long endTime) {
		source.initializeForTimePeriod(startTime, endTime);
	}
}
