package c3po.bitstamp;

import c3po.*;
import c3po.node.INode;
import c3po.utils.SignalMath;

/* Todo:
 * - Implement server timeout strategy (extrapolation for a little while, then crisis mode)
 * - Implement high frequency polling to avoid server update misses
 * - Prevent subclasses from accessing buffer directly to prevent programmer error
 * 		- Polling should return a ServerSampleEntry
 */

public abstract class BitstampTickerSource extends AbstractTickable implements INode {
	protected final int numSignals = 6;
	protected OutputSignal[] signals;
	protected boolean isEmpty = false;
	protected final long interpolationTime;
	protected final CircularArrayList<ServerSampleEntry> buffer;
	
	public BitstampTickerSource(long timestep, long interpolationTime) {
		super(timestep);
		
		this.interpolationTime = interpolationTime;
		
		this.signals = new OutputSignal[numSignals];
		for (int i = 0; i < numSignals; i++) {
			this.signals[i] = new OutputSignal(this);
		}
		
		int bufferLength = (int)Math.round(interpolationTime / timestep) + 1;
		buffer = new CircularArrayList<ServerSampleEntry>(bufferLength);
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
		ServerSampleEntry oldestEntry = buffer.get(0);
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
			ServerSampleEntry oldEntry = buffer.get(i);
			
			if (clientTimestamp < oldEntry.timestamp) {
				ServerSampleEntry newEntry = buffer.get(i+1);
				
				for (int j = 0; j < signals.length; j++) {
					Sample sample = SignalMath.lerp(oldEntry.get(j), newEntry.get(j), clientTimestamp);
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
 		
		ServerSampleEntry newestEntry = buffer.get(buffer.size()-1);
		
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

	public OutputSignal getOutputBid() {
		return signals[SignalName.BID.ordinal()];
	}
	
	public OutputSignal getOutputAsk() {
		return signals[SignalName.ASK.ordinal()];
	}
	
	public OutputSignal getOutputVolume() {
		return signals[SignalName.VOLUME.ordinal()];
	}
	
	public OutputSignal getOutputLast() {
		return signals[SignalName.LAST.ordinal()];
	}
	
	public OutputSignal getOutputHigh() {
		return signals[SignalName.HIGH.ordinal()];
	}
	
	public OutputSignal getOutputLow() {
		return signals[SignalName.LOW.ordinal()];
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
	
	public enum SignalName {
		LAST,
	    HIGH,
	    LOW,
	    VOLUME,
	    BID,
	    ASK
	}
	
	public class ServerSampleEntry {
		public final long timestamp;
		public final Sample[] samples;
		
		public ServerSampleEntry(long timestamp, int length) {
			this.timestamp = timestamp;
			this.samples = new Sample[length];
		}
		
		public int size() {
			return samples.length;
		}
		
		public Sample get(int i) {
			return samples[i];
		}
		
		public void set(int i, Sample sample) {
			samples[i] = sample;
		}

		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + (int) (timestamp ^ (timestamp >>> 32));
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ServerSampleEntry other = (ServerSampleEntry) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (timestamp != other.timestamp)
				return false;
			return true;
		}

		private BitstampTickerSource getOuterType() {
			return BitstampTickerSource.this;
		}

		@Override
		public String toString() {
			return "ServerSampleEntry [timestamp=" + timestamp + "]";
		}
	}
}
