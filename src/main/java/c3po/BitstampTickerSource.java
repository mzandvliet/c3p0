package c3po;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import au.com.bytecode.opencsv.CSVReader;
import c3po.BitstampTickerCsvSource.SignalName;

public abstract class BitstampTickerSource implements INode {
	protected final int numSignals = 6;
	protected OutputSignal[] signals;
	protected long lastTick = -1;
	protected boolean isEmpty = false;
	
	public BitstampTickerSource() {
		this.signals = new OutputSignal[numSignals];
		for (int i = 0; i < numSignals; i++) {
			this.signals[i] = new OutputSignal(this);
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
	public abstract void open();
	
	/**
	 * Close the source again
	 */
	public abstract void close();

	/**
	 * Tick is used to tell to source to fill his output 
	 * signal buffers with data that is closest to the
	 * supplied timestamp.
	 * 
	 */
	public abstract void tick(long timestamp);
	
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
}
