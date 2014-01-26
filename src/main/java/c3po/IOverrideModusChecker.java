package c3po;

import c3po.ITickable;

/**
 * Periodically fetches the bot override modus. This can be set using
 * the web application and can alter the bot's decision model.
 */
public interface IOverrideModusChecker extends ITickable {
		
	public enum OverrideModus {
	  NONE("none"),
	  SELL_MAX("sell_max"),
	  DONT_BUY("dont_buy"),
	  DO_NOTHING("do_nothing"),
	  DONT_SELL("dont_sell"),
	  BUY_MAX("buy_max");
	  
	  String modus;
	  
	  private OverrideModus(String modus) {
	    this.modus = modus;
	  }
	  
	  public static OverrideModus getById(String modus) {
		    for(OverrideModus e : values()) {
		        if(e.modus.equals(modus)) return e;
		    }
		    return null;
		 }
	}
	
	public OverrideModus getOverrideModus();
	
	public boolean mayBuy();
	public boolean mayTrade();
	public boolean maySell();
	public boolean mustSell();
	public boolean mustBuy();
}
