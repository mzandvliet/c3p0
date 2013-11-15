package c3po;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import au.com.bytecode.opencsv.CSVReader;

public class BitstampTickerCsvSource implements INode {
	private final int numSignals = 6;
	private OutputSignal[] signals;
	private final String path;
	private CSVReader reader;
	private long lastTick = -1;
	private boolean isEmpty = false;
	
	public BitstampTickerCsvSource(String path) {
		this.path = path;
		this.signals = new OutputSignal[numSignals];
		for (int i = 0; i < numSignals; i++) {
			this.signals[i] = new OutputSignal(this);
		}
	}
	
	@Override
	public int getNumOutputs() {
		return signals.length;
	}
	
	@Override
	public ISignal getOutput(int i) {
		return signals[i];
	}

	public void open() {
		try {
			reader = new CSVReader(new FileReader(path));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void close() {
		try {
			reader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void tick(long tick) {
		if (tick >= lastTick) {
			parseCsv();
			lastTick = tick;
		}
	}

	private void parseCsv() {
	    try {
	    	String [] nextLine  = reader.readNext();
	    	if (nextLine != null) {
	    		long timestamp = Long.parseLong(nextLine[0]);
	    		for (int i = 0; i < signals.length; i++) {
	    			signals[i].setSample(new Sample(timestamp, Double.parseDouble(nextLine[i])));
	    		}
			}
	    	else {
	    		 isEmpty = true;
	    	}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Method that indicates whether the CSV has reached it's end
	 * and has no new data. (Assuming that CSV's are static now and
	 * not dynamically filled mid-execution)
	 *  
	 * @return Whether or not the CSV is empty
	 */
	public boolean isEmpty() {
		return isEmpty;
	}
	
	public enum SignalName {
		LAST,
	    HIGH,
	    LOW,
	    VOLUME,
	    BID,
	    ASK
	}
}
