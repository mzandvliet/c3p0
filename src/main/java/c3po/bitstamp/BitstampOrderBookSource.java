package c3po.bitstamp;

import c3po.*;
import c3po.utils.SignalMath;

/**
 * This class is quite the copy pasta from the BitstampTickerSource.
 * TODO Avoid duplication between the two. High prio.
 */
public abstract class BitstampOrderBookSource extends AbstractTickable implements IBitstampOrderBookSource {
	protected final int numSignals = OrderBookSignal.values().length;
	protected final OutputSignal[] signals;
	protected final long interpolationTime;
	protected final CircularArrayList<ServerSnapshot> buffer;
	protected boolean isEmpty = false;
	
	public BitstampOrderBookSource(long timestep, long interpolationTime) {
		super(timestep);
		
		this.interpolationTime = interpolationTime;
		
		this.signals = new OutputSignal[numSignals];
		for (int i = 0; i < numSignals; i++) {
			this.signals[i] = new OutputSignal(this, OrderBookSignal.values()[i].toString());
		}
		
		int bufferLength = (int)Math.round(interpolationTime / timestep) + 1;
		buffer = new CircularArrayList<ServerSnapshot>(bufferLength);
	}
	
	@Override
	public void onNewTick(long clientTimestamp) {
		pollServer(clientTimestamp);
		updateOutputs(clientTimestamp);
	}
	
	protected abstract void pollServer(long clientTimestamp);

	private void updateOutputs(long clientTimestamp) {
		/*
		 *  If clientTime is older than most recent server entry (which happens at
		 *   startup), just return the oldest possible value. This results in a
		 *   constant signal until server start time is reached.
		 */
		ServerSnapshot oldestEntry = buffer.get(0);
		if (clientTimestamp <= oldestEntry.timestamp) {
			for (int j = 0; j < signals.length; j++) {
				Sample sample = oldestEntry.get(j);
				signals[j].setSample(sample);
			}
			return;
		}
		
		/*
		 *  If client time falls within the buffered entries, interpolate the result
		 */
 		for (int i = 0; i < buffer.size()-1; i++) {
 			ServerSnapshot oldEntry = buffer.get(i);
			
			if (clientTimestamp < oldEntry.timestamp) {
				ServerSnapshot newEntry = buffer.get(i+1);
				
				for (int j = 0; j < signals.length; j++) {
					Sample sample = SignalMath.interpolate(oldEntry.get(j), newEntry.get(j), clientTimestamp);
					signals[j].setSample(sample);
				}
				
				return;
			}
		}
 		
 		/*
		 * Todo:
		 * 
		 * if client time is newer than server time (in case of network error or something) we
		 * should do error handling. Either extrapolate and hope you regain connection or go
		 *  into crisis mode. For now, just poop out the last available value...
		 */
 		
 		ServerSnapshot newestEntry = buffer.get(buffer.size()-1);
		
		for (int j = 0; j < signals.length; j++) {
			Sample sample = newestEntry.get(j);
			signals[j].setSample(sample);
		}
	}

	@Override
	public int getNumOutputs() {
		return signals.length;
	}
	
	@Override
	public ISignal getOutput(int i) {
		return signals[i];
	}
	
	@Override
	public void reset() {
		super.reset();
		
		buffer.clear();
		
		for (OutputSignal signal : signals) {
			signal.setSample(Sample.none);
		}
	}
	
	public ISignal getOutputVolumeBid() {
		return signals[OrderBookSignal.VOLUME_BID.ordinal()];
	}

	public ISignal getOutputVolumeAsk() {
		return signals[OrderBookSignal.VOLUME_ASK.ordinal()];
	}

	public ISignal getOutputP99Bid() {
		return signals[OrderBookSignal.P99_BID.ordinal()];
	}

	public ISignal getOutputP98Bid() {
		return signals[OrderBookSignal.P98_BID.ordinal()];
	}

	public ISignal getOutputP97Bid() {
		return signals[OrderBookSignal.P97_BID.ordinal()];
	}

	public ISignal getOutputP96Bid() {
		return signals[OrderBookSignal.P96_BID.ordinal()];
	}

	public ISignal getOutputP95Bid() {
		return signals[OrderBookSignal.P95_BID.ordinal()];
	}
	
	public ISignal getOutputP90Bid() {
		return signals[OrderBookSignal.P90_BID.ordinal()];
	}
	
	public ISignal getOutputP85Bid() {
		return signals[OrderBookSignal.P85_BID.ordinal()];
	}
	
	public ISignal getOutputP80Bid() {
		return signals[OrderBookSignal.P80_BID.ordinal()];
	}
	
	public ISignal getOutputP75Bid() {
		return signals[OrderBookSignal.P75_BID.ordinal()];
	}

	public ISignal getOutputP99Ask() {
		return signals[OrderBookSignal.P99_ASK.ordinal()];
	}

	public ISignal getOutputP98Ask() {
		return signals[OrderBookSignal.P98_ASK.ordinal()];
	}

	public ISignal getOutputP97Ask() {
		return signals[OrderBookSignal.P97_ASK.ordinal()];
	}

	public ISignal getOutputP96Ask() {
		return signals[OrderBookSignal.P96_ASK.ordinal()];
	}

	public ISignal getOutputP95Ask() {
		return signals[OrderBookSignal.P95_ASK.ordinal()];
	}
	
	public ISignal getOutputP90Ask() {
		return signals[OrderBookSignal.P90_ASK.ordinal()];
	}
	
	public ISignal getOutputP85Ask() {
		return signals[OrderBookSignal.P85_ASK.ordinal()];
	}
	
	public ISignal getOutputP80Ask() {
		return signals[OrderBookSignal.P80_ASK.ordinal()];
	}
	
	public ISignal getOutputP75Ask() {
		return signals[OrderBookSignal.P75_ASK.ordinal()];
	}

	/**
	 * Prepare the source for handling ticks
	 */
	public abstract boolean open();
	
	/**
	 * Close the source again
	 */
	public abstract boolean close();

	/**
	 * Method that indicates whether the source has reached it's end
	 * and has no new data.
	 *  
	 * @return Whether or not the source is empty
	 */
	public abstract boolean isEmpty();
}
