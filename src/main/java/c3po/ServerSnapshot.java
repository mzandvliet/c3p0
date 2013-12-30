package c3po;

import java.util.Arrays;

import c3po.bitstamp.BitstampTickerSource;

public class ServerSnapshot {
	public final long timestamp;
	public final Sample[] samples;
	
	public ServerSnapshot(long timestamp, int length) {
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
		result = prime * result + Arrays.hashCode(samples);
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
		ServerSnapshot other = (ServerSnapshot) obj;
		if (!Arrays.equals(samples, other.samples))
			return false;
		if (timestamp != other.timestamp)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "ServerSampleEntry [timestamp=" + timestamp + "]";
	}
}