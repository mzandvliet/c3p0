package c3po.node;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Test;
import org.mockito.Mockito;

import c3po.ISignal;
import c3po.Sample;
import c3po.TradeIntention;
import c3po.TradeIntention.TradeActionType;

public class LossStopNodeTest {

	@Test
	public void testCalculateTradeAdvice() {
		assertEquals(-0.5d, LossStopNode.calculateTradeAdvice(100.0, 90.0, 5, 15), 0.00001d);
		assertEquals(0d, LossStopNode.calculateTradeAdvice(100.0, 96.0, 5, 15), 0.00001d);
		assertEquals(-1d, LossStopNode.calculateTradeAdvice(100.0, 80.0, 5, 15), 0.00001d);
	}
	
	@Test
	public void testAll() {
		// Mock a price signal
	    ISignal priceSignal = Mockito.mock(ISignal.class);
	    when(priceSignal.getSample(any(Long.class)))
 	          .thenReturn(new Sample(1000, 90))
 	          .thenReturn(new Sample(2000, 100))
	          .thenReturn(new Sample(3000, 105))
	          .thenReturn(new Sample(4000, 120))
	          .thenReturn(new Sample(5000, 110));
	    
	    LossStopNode lossStopNode = new LossStopNode(1000l, priceSignal, new LossStopNodeConfig(5d, 15d));
	    lossStopNode.tick(1000);
	    lossStopNode.tick(2000);
	    
	    // Nothing bought yet so no advice
	    assertEquals(0.0d, lossStopNode.getOutputTradeAdvice().getSample(2000).value, 0.0001d);
	    
	    // Buy something
	    lossStopNode.onTrade(new TradeIntention(TradeActionType.BUY, 2000, 12));
	    
	    lossStopNode.tick(3000);
	    assertEquals(0.0d, lossStopNode.getOutputTradeAdvice().getSample(3000).value, 0.0001d);
	    
	    lossStopNode.tick(4000);
	    assertEquals(0.0d, lossStopNode.getOutputTradeAdvice().getSample(4000).value, 0.0001d);
	    
	    // Price has gone down!
	    lossStopNode.tick(5000);
	    assertEquals(-0.33d, lossStopNode.getOutputTradeAdvice().getSample(5000).value, 0.01d);
	}
}
