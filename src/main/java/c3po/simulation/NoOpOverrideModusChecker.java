package c3po.simulation;

import c3po.IOverrideModusChecker;

public class NoOpOverrideModusChecker implements IOverrideModusChecker {

	@Override
	public long getTimestep() {
		return 0;
	}

	@Override
	public long getLastTick() {
		return 0;
	}

	@Override
	public void tick(long tick) {

	}

	@Override
	public OverrideModus getOverrideModus() {
		return OverrideModus.NONE;
	}

	@Override
	public boolean mayBuy() {
		return true;
	}

	@Override
	public boolean mayTrade() {
		return true;
	}

	@Override
	public boolean maySell() {
		return true;
	}

	@Override
	public boolean mustSell() {
		return false;
	}

	@Override
	public boolean mustBuy() {
		return false;
	}

}
