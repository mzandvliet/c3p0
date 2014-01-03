package c3po.orderbook;

import java.util.ArrayList;
import java.util.List;

import ch.qos.logback.classic.Logger;
import c3po.*;
import c3po.utils.SignalMath;

/**
 * This class is quite the copy pasta from the BitstampTickerSource.
 * TODO Avoid duplication between the two. High prio.
 */
public abstract class OrderBookPercentileTransformer extends AbstractTickable implements IOrderBookPercentileTransformer {
	protected final OutputSignal bidVolumeSignal;
	protected final OutputSignal askVolumeSignal;
	protected final List<OutputSignal> bidPercentileSignals;
	protected final List<OutputSignal> askPercentileSignals;
	protected final long interpolationTime;
	protected final CircularArrayList<ServerSnapshot> buffer;
	protected boolean isEmpty = false;
	
	protected final double[] percentiles;
	
	private final List<OutputSignal> signals;
	
	public OrderBookPercentileTransformer(long timestep, long interpolationTime, double[] percentiles) {
		super(timestep);
		
		this.interpolationTime = interpolationTime;
		this.percentiles = percentiles;
		
		this.bidVolumeSignal = new OutputSignal(this, "bid_volume");
		this.askVolumeSignal = new OutputSignal(this, "ask_volume");
		
		this.bidPercentileSignals = new ArrayList<OutputSignal>(percentiles.length);
		this.askPercentileSignals = new ArrayList<OutputSignal>(percentiles.length);
		
		for (int i = 0; i < percentiles.length; i++) {
			this.bidPercentileSignals.add(new OutputSignal(this, String.format("p%s_bid", percentiles[i])));
			this.askPercentileSignals.add(new OutputSignal(this, String.format("p%s_ask", percentiles[i])));
		}
		
		signals = new ArrayList<OutputSignal>();
		signals.add(bidVolumeSignal);
		signals.add(askVolumeSignal);
		signals.addAll(bidPercentileSignals);
		signals.addAll(askPercentileSignals);
		
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
		if (buffer.size() == 0)
			throw new IllegalStateException("The interpolation buffer is empty.");
		
		/*
		 *  If clientTime is older than most recent server entry (which happens at
		 *   startup), just return the oldest possible value. This results in a
		 *   constant signal until server start time is reached.
		 */
		
		ServerSnapshot oldestEntry = buffer.get(0);
		if (clientTimestamp <= oldestEntry.timestamp) {
			for (int j = 0; j < signals.size(); j++) {
				Sample sample = oldestEntry.get(j);
				signals.get(j).setSample(sample);
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
				
				for (int j = 0; j < signals.size(); j++) {
					Sample sample = SignalMath.interpolate(oldEntry.get(j), newEntry.get(j), clientTimestamp);
					signals.get(j).setSample(sample);
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
		
		for (int j = 0; j < signals.size(); j++) {
			Sample sample = newestEntry.get(j);
			signals.get(j).setSample(sample);
		}
	}

	@Override
	public int getNumOutputs() {
		return signals.size();
	}
	
	@Override
	public ISignal getOutput(int i) {
		return signals.get(i);
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
		return bidVolumeSignal;
	}

	public ISignal getOutputVolumeAsk() {
		return askVolumeSignal;
	}

	public ISignal getOutputBidPercentile(int percentileIndex) {
		return bidPercentileSignals.get(percentileIndex);
	}
	
	public ISignal getOutputAskPercentile(int percentileIndex) {
		return askPercentileSignals.get(percentileIndex);
	}
}
