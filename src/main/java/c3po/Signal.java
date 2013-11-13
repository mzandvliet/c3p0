package c3po;

/*
 * Behaves like a value type / struct
 */
public class Signal {
	public long timestamp;
	public double value;
	
	public Signal() {
		timestamp = 0;
		value = 0;
	}
	
	public Signal(long timestamp, double value) {
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
		Signal other = (Signal) obj;
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
}