package c3po.simulation;

import c3po.ISignal;

/**
 * Classes that implement this can give a trade advice
 * to any bot that uses it.
 */
public interface ITradeAdviceSignal {
	
	/**
	 * An TradeAdvice signal contains a value between the
	 * -1 (Strong sell advice) to 0 (neutral) to 1 (Strong buy advice)
	 * 
	 * @return Trade Advice
	 */
	public ISignal getOutputTradeAdvice();
}
