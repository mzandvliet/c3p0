package c3po;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import c3po.structs.TradeIntention;

public class DebugTradeLogger implements ITradeListener {
	private static final Logger LOGGER = LoggerFactory.getLogger(DebugTradeLogger.class);
	
	private List<TradeIntention> actions;
	
	public DebugTradeLogger() {
		this.actions = new ArrayList<TradeIntention>();
	}
	
	@Override
	public void onTrade(TradeIntention action) {
		actions.add(action);
	}
	
	public List<TradeIntention> getActions() {
		return actions;
	}
	
	public void writeLog() {
		LOGGER.debug("Trades: " + actions.size());
		
		for(TradeIntention action : actions) {
			LOGGER.debug(action.toString());
		}
	}
}
