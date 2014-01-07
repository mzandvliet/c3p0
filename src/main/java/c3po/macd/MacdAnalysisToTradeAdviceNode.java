package c3po.macd;

import c3po.*;
import c3po.node.ExpMovingAverageNode;
import c3po.node.INode;
import c3po.node.SubtractNode;
import c3po.simulation.ITradeAdviceSignal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 
 * This class is a transformer from a macdDiff to a tradeAdvice.
 */
public class MacdAnalysisToTradeAdviceNode extends AbstractTickable implements INode, ITradeAdviceSignal {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(MacdAnalysisToTradeAdviceNode.class);
	
	private final ISignal macdDiff;
	private final OutputSignal output;
	

	private double positiveMacdMultiplier = 0.2;
	private double negativeMacdMultiplier = 0.2;
	
	public MacdAnalysisToTradeAdviceNode(long timestep, ISignal macdDiff, double positiveMacdMultiplier, double negativeMacdMultiplier) {
		super(timestep);
		
		this.macdDiff = macdDiff;
		this.positiveMacdMultiplier = positiveMacdMultiplier;
		this.negativeMacdMultiplier = negativeMacdMultiplier;
		
		this.output = new OutputSignal(this, "TradeAdvice");
	}
	
	@Override
	public int getNumOutputs() {
		return 1;
	}
	
	@Override
	public ISignal getOutput(int i) {
		return output;
	}
	
	@Override
	public void update(long tick) {
		Sample newest = calculateTradeAdvice(macdDiff.getSample(tick));
		output.setSample(newest);
	}

	/**
	 * This method takes the macdDiff and turns it into a tradeAdvice.
	 * 
	 * @param macdDiff
	 * @return TradeAdvice
	 */
	private Sample calculateTradeAdvice(Sample macdDiff) {
		double advice = 0;
		if(macdDiff.value > 0) {
			advice = Math.min(macdDiff.value * positiveMacdMultiplier, 1);
		} else if(macdDiff.value < 0) {
			advice = Math.max(macdDiff.value * negativeMacdMultiplier, -1);
		}
		
		return new Sample(macdDiff.timestamp, advice);
	}
	
	public enum SignalNames {
		TRADE_ADVICE,
	}

	@Override
	public ISignal getOutputTradeAdvice() {
		return output;
	}
}
