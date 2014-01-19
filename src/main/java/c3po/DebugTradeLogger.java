package c3po;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import c3po.structs.TradeResult;

public class DebugTradeLogger implements ITradeListener {
	private static final Logger LOGGER = LoggerFactory.getLogger(DebugTradeLogger.class);
	
	private List<TradeResult> actions;
	
	public DebugTradeLogger() {
		this.actions = new ArrayList<TradeResult>();
	}
	
	@Override
	public void onTrade(TradeResult action) {
		actions.add(action);
	}
	
	public List<TradeResult> getActions() {
		return actions;
	}
	
	public void writeLog() {
		LOGGER.debug("Trades: " + actions.size());
		
		for(TradeResult action : actions) {
			LOGGER.debug(action.toString());
		}
	}
}
