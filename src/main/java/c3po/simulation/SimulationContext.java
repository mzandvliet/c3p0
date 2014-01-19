package c3po.simulation;

import java.util.List;

import c3po.*;
import c3po.clock.ISimulationClock;
import c3po.wallet.IWallet;

public class SimulationContext {
	private final List<INonRealtimeSource> sources;
	private final ISimulationClock clock;
	private final ISignal priceSignal;
	private final ISignal volumeSignal;
	private final ITradeFloor tradeFloor;
	private final IWallet wallet;
	
	private long startTime;
	private long endTime;
	
	public SimulationContext(
			List<INonRealtimeSource> sources,
			ISimulationClock clock,
			ISignal price,
			ISignal volume,
			ITradeFloor tradeFloor,
			IWallet wallet
			) {
		this.sources = sources;
		this.clock = clock;
		this.priceSignal = price;
		this.volumeSignal = volume;
		this.tradeFloor = tradeFloor;
		this.wallet = wallet;
	}

	public List<INonRealtimeSource> getSources() {
		return sources;
	}

	public ISignal getPriceSignal() {
		return priceSignal;
	}
	
	public ISignal getVolumeSignal() {
		return volumeSignal;
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
	
	public void setSimulationRange(long startTime, long endTime) {
		this.startTime = startTime;
		this.endTime = endTime;
		
		for(INonRealtimeSource source : sources) {
			source.setSimulationRange(startTime, endTime);
		}
	}
	
	public void run() {
		clock.run(startTime, endTime);
	}
	
	public void reset() {
		for(INonRealtimeSource source : sources) {
			source.reset();
		}
		
		tradeFloor.reset();
	}
}
