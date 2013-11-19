package c3po;

public class WalletTransactionResult {
	public final double timestamp;
	public final double usdTotal;
	public final double btcTotal;
	
	public WalletTransactionResult(double timestamp, double usdTotal, double btcTotal) {
		this.timestamp = timestamp;
		this.usdTotal = usdTotal;
		this.btcTotal = btcTotal;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(btcTotal);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(timestamp);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(usdTotal);
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
		WalletTransactionResult other = (WalletTransactionResult) obj;
		if (Double.doubleToLongBits(btcTotal) != Double
				.doubleToLongBits(other.btcTotal))
			return false;
		if (Double.doubleToLongBits(timestamp) != Double
				.doubleToLongBits(other.timestamp))
			return false;
		if (Double.doubleToLongBits(usdTotal) != Double
				.doubleToLongBits(other.usdTotal))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "WalletTransaction [timestamp=" + timestamp + ", usdTotal="
				+ usdTotal + ", btcTotal=" + btcTotal + "]";
	}
}
