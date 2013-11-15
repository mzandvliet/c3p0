package c3po;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is used to repeatedly run a bit with different configurations and analyse its performance
 */

public class BotTrainer {
	private static final Logger LOGGER = LoggerFactory.getLogger(Bot.class);


	private final static List<Bot> bots = new LinkedList<Bot>();
	
	public static void main(String[] args) throws ClassNotFoundException, SQLException {
		
		// Run the bots a 100 times
		for(int i = 0; i < 300; i++) {
			Bot bot = new Bot(false, 14000, 1000.0, 1000);
			bot.run();
			
			bots.add(bot);
		}
		
		double highestWalletValue = 0;
		Bot bestBot = null;
		for(Bot bot : bots) {
			double walletValue = bot.getTradeFloor().getWalletValue();
			
			if(walletValue > highestWalletValue){
				highestWalletValue = walletValue;
				bestBot = bot;
			}
				
		}
		
		LOGGER.debug("Bot " + bestBot + " had " + highestWalletValue + " USD");	
	}
}
