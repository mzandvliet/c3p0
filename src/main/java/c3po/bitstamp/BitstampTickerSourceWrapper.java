package c3po.bitstamp;

import c3po.ISignal;
import c3po.SignalWrapper;
import c3po.TickerSignal;

public class BitstampTickerSourceWrapper implements IBitstampTickerSource {
	private static final int numSignals = 6;
	
	private final SignalWrapper[] signals;
	
	private IBitstampTickerSource source;
	
	
	public BitstampTickerSourceWrapper() {
		this.signals = new SignalWrapper[numSignals];
		for (int i = 0; i < numSignals; i++) {
			this.signals[i] = new SignalWrapper();
		}
	}
	
	public void setActualSource(IBitstampTickerSource source) {
		this.source = source;
		
		for (int i = 0; i < numSignals; i++) {
			this.signals[i].setInput(source.getOutput(i));
		}
	}
	
	@Override
	public int getNumOutputs() {
		return numSignals;
	}

	@Override
	public ISignal getOutput(int i) {
		return source.getOutput(i);
	}

	@Override
	public long getTimestep() {
		return source.getTimestep();
	}

	@Override
	public long getLastTick() {
		return source.getLastTick();
	}

	@Override
	public void tick(long tick) {
		source.tick(tick);
	}

	@Override
	public ISignal getOutputBid() {
		return signals[TickerSignal.BID.ordinal()];
	}
	
	@Override
	public ISignal getOutputAsk() {
		return signals[TickerSignal.ASK.ordinal()];
	}
	
	@Override
	public ISignal getOutputVolume() {
		return signals[TickerSignal.VOLUME.ordinal()];
	}
	
	@Override
	public ISignal getOutputLast() {
		return signals[TickerSignal.LAST.ordinal()];
	}
	
	@Override
	public ISignal getOutputHigh() {
		return signals[TickerSignal.HIGH.ordinal()];
	}
	
	@Override
	public ISignal getOutputLow() {
		return signals[TickerSignal.LOW.ordinal()];
	}
}
