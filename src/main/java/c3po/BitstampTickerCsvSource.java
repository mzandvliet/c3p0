package c3po;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import au.com.bytecode.opencsv.CSVReader;

public class BitstampTickerCsvSource extends BitstampTickerSource {
	private final String path;
	private CSVReader reader;
	private long lastTick = -1;
	private boolean isEmpty = false;
	
	public BitstampTickerCsvSource(String path) {
		super();
		this.path = path;
	}
	
	public void open() {
		try {
			reader = new CSVReader(new FileReader(path));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	public void close() {
		try {
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void tick(long tick) {
		if (tick > lastTick) {
			parseCsv();
		}
		lastTick = tick;
	}

	private void parseCsv() {
	    try {
	    	String [] nextLine  = reader.readNext();
	    	if (nextLine != null) {
	    		long timestamp = Long.parseLong(nextLine[0]);

	    		for (int i = 0; i < signals.length; i++) {
	    			// Map CSV fields (+1 to skip timestamp) to the signals 
	    			signals[i].setSample(new Sample(timestamp, Double.parseDouble(nextLine[i+1])));
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
