package c3po.macd;

import c3po.*;
import c3po.Training.*;

/*
 * Todo:
 * 
 * This wouldn't be necessary if you could create bots without supplying ticker, tradefloor, wallet, etc.
 * AND if you could create instances of generic types, which you can't.
 */

public class MacdBotFactory implements IBotFactory<MacdBotConfig> {

	private final IWallet walletPrototype;
	private final ISignal ticker;
	private final ITradeFloor tradeFloor;
	
	public MacdBotFactory(IWallet walletPrototype, final ISignal ticker, final ITradeFloor tradeFloor) {
		super();
		this.walletPrototype = walletPrototype;
		this.ticker = ticker;
		this.tradeFloor = tradeFloor;
	}

	@Override
	public MacdBot create(MacdBotConfig config) {
		return new MacdBot(config, ticker, walletPrototype.copy(), tradeFloor);
	}
}
