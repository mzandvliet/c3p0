package c3po;

public class SignalWrapper implements ISignal {
	private ISignal input;
	
	@Override
	public Sample getSample(long tick) {
		return input.getSample(tick);
	}
	
	@Override
	public String getName() {
		return input.getName();
	}
	
	public String toString() {
		return "Wrapped Signal: "  + input.toString();
	}
	
	public void setInput(ISignal input) {
		this.input = input;
	}
}