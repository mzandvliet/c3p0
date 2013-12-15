package c3po.wallet;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Wallet implements IWallet {
	private static final Logger LOGGER = LoggerFactory.getLogger(Wallet.class);
	
	private List<IWalletTransactionListener> walletListeners;
	
	private double walletUsd;
	private double walletBtc;
	
	public Wallet(double walletUsd, double walletBtc) {
		this.walletListeners = new ArrayList<IWalletTransactionListener>();
		this.walletUsd = walletUsd;
		this.walletBtc = walletBtc;
	}

	@Override
	public double getWalletUsd() {
		return walletUsd;
	}

	@Override
	public double getWalletBtc() {
		return walletBtc;
	}

	@Override
	public boolean transact(long timestamp, double usd, double btc) {
		walletUsd += usd;
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

	@Override
	public void update(double dollars, double btc) {
		if(dollars != walletUsd  || btc != walletBtc) {
			LOGGER.info("Received update for the wallet. Difference " + (dollars - walletUsd) + " USD, " + (btc - walletBtc) + " BTC");
			walletUsd = dollars;
			walletBtc = btc;
		}
	}

	@Override
	public IWallet copy() {
		return new Wallet(walletUsd, walletBtc);
	}
}
