package c3po;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DebugTradeLogger implements ITradeListener {
	private static final Logger LOGGER = LoggerFactory.getLogger(DebugTradeLogger.class);
	
	private List<TradeAction> actions;
	
	public DebugTradeLogger() {
		this.actions = new ArrayList<TradeAction>();
	}
	
	@Override
	public void onTrade(TradeAction action) {
		actions.add(action);
	}
	
	public List<TradeAction> getActions() {
		return actions;
	}
	
	public void writeLog() {
		LOGGER.debug("Trades: " + actions.size());
		
		for(TradeAction action : actions) {
			LOGGER.debug(action.toString());
		}
	}
}
