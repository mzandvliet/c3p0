package c3po;

public interface IWallet {
	public double getWalledUsd();
	public double getWalletBtc();
	
	public boolean transact(long timestamp, double dollars, double btc);
	
	public void addListener(IWalletTransactionListener listener);
	public void removeListener(IWalletTransactionListener listener);
}
