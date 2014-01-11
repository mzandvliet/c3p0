package c3po.wallet;

public interface IWallet {
	public double getUsdTotal();
	public double getBtcTotal();
	public double getUsdAvailable();
	public double getBtcAvailable();
	public double getUsdReserved();
	public double getBtcReserved();
	public void setConfigUsdReserved(double configUsdReserved);
	
	/**
	 * Update the current contents of the wallet.
	 * Triggers a notification.
	 * 
	 * @param timestamp Epoch in milliseconds
	 * @param dollarsAvailable
	 * @param btcAvailable
	 * @param dollarsReserved
	 * @param btcReserved
	 */
	public void update(long timestamp, double dollarsAvailable, double btcAvailable, double dollarsReserved, double btcReserved);
	
	/**
	 * Modify the current available of the wallet.
	 * Triggers a notification.
	 * 
	 * @param timestamp Epoch in milliseconds
	 * @param dollarsAvailable
	 * @param btcAvailable
	 */
	public void modify(long timestamp, double dollarsAvailable, double btcAvailable);
	
	/**
	 * Reserve some funds that need to be reserved for an open order.
	 * Does not trigger a notification.
	 * 
	 * @param reserveUsd The amount of USD to reserve
	 * @param reserveBtc The amount of BTC to reserve
	 * @throws Exception
	 */
	public void reserve(double reserveUsd, double reserveBtc) throws Exception;
	
	public void addListener(IWalletUpdateListener listener);
	public void removeListener(IWalletUpdateListener listener);
	
	public IWallet copy();
}
