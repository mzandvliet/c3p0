package c3po;

/*
 * Meant to be used like a value type.
 * 
 * (What the shit, java? This is a fucking tuple.)
 */
public class Sample {
	public static final Sample none = new Sample(0, 0.0);
	
	public final long timestamp;
	public final double value;
	
	public Sample(long timestamp, double value) {
		this.timestamp = timestamp;
		this.value = value;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (timestamp ^ (timestamp >>> 32));
		long temp;
		temp = Double.doubleToLongBits(value);
		result = prime * result + (int) (temp ^ (temp >>> 32));
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
		Sample other = (Sample) obj;
		if (timestamp != other.timestamp)
			return false;
		if (Double.doubleToLongBits(value) != Double
				.doubleToLongBits(other.value))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Signal [timestamp=" + timestamp + ", value=" + value + "]";
	}
	
	public static Sample copy(Sample signal) {
		return new Sample(signal.timestamp, signal.value);
	}
}