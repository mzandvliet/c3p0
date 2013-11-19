package c3po;

import java.util.ArrayList;
import java.util.List;

public class Wallet implements IWallet {
	private List<IWalletTransactionListener> walletListeners;
	
	private double walletUsd;
	private double walletBtc;
	
	
	
	public Wallet(double walletUsd, double walletBtc) {
		this.walletListeners = new ArrayList<IWalletTransactionListener>();
		this.walletUsd = walletUsd;
		this.walletBtc = walletBtc;
	}

	@Override
	public double getWalledUsd() {
		return walletUsd;
	}

	@Override
	public double getWalletBtc() {
		return walletBtc;
	}

	@Override
	public boolean transact(long timestamp, double dollars, double btc) {
		walletUsd += dollars;
		walletBtc += btc;
		notify(new WalletTransactionResult(timestamp, walletUsd, walletBtc));
		return true;
	}
	
	private void notify(WalletTransactionResult result) {
		for (IWalletTransactionListener listener : walletListeners) {
			listener.onWalletTransaction(result);
		}
	}

	@Override
	public void addListener(IWalletTransactionListener listener) {
		walletListeners.add(listener);
	}

	@Override
	public void removeListener(IWalletTransactionListener listener) {
		walletListeners.remove(listener);
	}
	
	@Override
	public String toString() {
		return "Wallet: " + walletUsd + " USD, " + walletBtc + " BTC";
	}
}
