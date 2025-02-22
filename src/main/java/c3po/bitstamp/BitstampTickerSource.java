package c3po.bitstamp;

import c3po.*;
import c3po.utils.SignalMath;

/* Todo:
 * - Implement server timeout strategy (extrapolation for a little while, then crisis mode)
 * - Implement high frequency polling to avoid server update misses
 * - Prevent subclasses from accessing buffer directly to prevent programmer error
 * 		- Polling should return a ServerSampleEntry
 */

public abstract class BitstampTickerSource extends AbstractTickable implements IBitstampTickerSource {
	protected final int numSignals = 6;
	protected final OutputSignal[] signals;
	protected final long interpolationTime;
	protected final CircularArrayList<ServerSnapshot> buffer;
	protected boolean isEmpty = false;
	
	public BitstampTickerSource(long timestep, long interpolationTime) {
		super(timestep);
		
		this.interpolationTime = interpolationTime;
		
		this.signals = new OutputSignal[numSignals];
		for (int i = 0; i < numSignals; i++) {
			this.signals[i] = new OutputSignal(this, TickerSignal.values()[i].toString());
		}
		
		int bufferLength = (int)Math.round(interpolationTime / timestep) + 1;
		buffer = new CircularArrayList<ServerSnapshot>(bufferLength);
	}
	
	@Override
	public void update(long clientTimestamp) {
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
