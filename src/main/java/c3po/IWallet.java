package c3po;

public interface IWallet {
	public double getWalletUsd();
	public double getWalletBtc();
	
	public void update(double dollars, double btc);
	public boolean transact(long timestamp, double dollars, double btc);
	
	public void addListener(IWalletTransactionListener listener);
	public void removeListener(IWalletTransactionListener listener);
	
	public IWallet copy();
}
