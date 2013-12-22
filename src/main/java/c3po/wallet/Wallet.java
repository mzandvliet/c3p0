package c3po.wallet;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generic Implementation of the Wallet class
 * currently used for USD/BTC currencies.
 * 
 * It makes a distinction between available and reserved amounts.
 * Bots that want to know how much they can spend should use
 * available. Use total for the sum of both, for instance to 
 * calculate current net worth.
 * 
 * Reserved is temporary for open orders.
 * 
 */
public class Wallet implements IWallet {
	private static final Logger LOGGER = LoggerFactory.getLogger(Wallet.class);
	
	private List<IWalletUpdateListener> walletListeners;
	
	protected double usdAvailable;
	protected double btcAvailable;
	private double usdReserved;
	private double btcReserved;
	
	public Wallet(double usdAvailable, double btcAvailable, double usdReserved, double btcReserved) {
		this.walletListeners = new ArrayList<IWalletUpdateListener>();
		this.usdAvailable = usdAvailable;
		this.usdReserved = usdReserved;
		this.btcAvailable = btcAvailable;
		this.btcReserved = btcReserved;
	}

	@Override
	public void reserve(double reserveUsd, double reserveBtc) throws Exception {
		if(usdAvailable < reserveUsd)
			throw new Exception("Could not reserve " + reserveUsd + "USD because the wallet only has " + usdAvailable);
		
		if(btcAvailable < reserveBtc)
			throw new Exception("Could not reserve " + reserveBtc + "USD because the wallet only has " + btcAvailable);
		
		usdAvailable -= reserveUsd;
		usdReserved += reserveUsd;
		
		btcAvailable -= reserveBtc;
		btcReserved += reserveBtc;
	}
	
	@Override
	public void update(long timestamp, double usdAvailable, double btcAvailable, double usdReserved, double btcReserved) {
		if(usdAvailable != this.usdAvailable  || btcAvailable != this.btcAvailable
		|| usdReserved != this.usdReserved  || btcReserved != this.btcReserved) {
			
			this.usdAvailable = usdAvailable;
			this.btcAvailable = btcAvailable;
			this.usdReserved = usdReserved;
			this.btcReserved = btcReserved;
			
			LOGGER.info("Received update for the wallet: "+ toString());
			notify(new WalletUpdateResult(timestamp, this.usdAvailable +this.usdReserved, this.btcAvailable - this.btcReserved));
		}
	}
	

	@Override
	public void modify(long timestamp, double usdAvailable, double btcAvailable) {
		update(timestamp, this.usdAvailable + usdAvailable, this.btcAvailable + btcAvailable, this.usdReserved, this.btcReserved);
	}
	
	protected void notify(WalletUpdateResult result) {
		for (IWalletUpdateListener listener : walletListeners) {
			listener.onWalletUpdate(result);
		}
	}

	@Override
	public void addListener(IWalletUpdateListener listener) {
		walletListeners.add(listener);
	}

	@Override
	public void removeListener(IWalletUpdateListener listener) {
		walletListeners.remove(listener);
	}
	
	@Override
	public String toString() {
		return "Wallet [Available " + usdAvailable + " USD / " + btcAvailable + " BTC], [Reserved " + usdReserved + " USD / " + btcReserved + " BTC],";
	}

	@Override
	public IWallet copy() {
		return new Wallet(usdAvailable, btcAvailable, usdReserved, btcReserved);
	}
	

	@Override
	public double getUsdAvailable() {
		return usdAvailable;
	}

	@Override
	public double getBtcAvailable() {
		return btcAvailable;
	}

	@Override
	public double getUsdTotal() {
		return usdAvailable + usdReserved;
	}

	@Override
	public double getBtcTotal() {
		return btcAvailable + btcReserved;
	}

	@Override
	public double getUsdReserved() {
		return usdReserved;
	}

	@Override
	public double getBtcReserved() {
		return btcReserved;
	}
}
