package c3po;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import au.com.bytecode.opencsv.CSVReader;

public class BitstampTickerCsvSource implements ISignalSource {
	private final String path;
	private CSVReader reader;
	private Signal latest;
	private long lastTick = -1;
	
	public BitstampTickerCsvSource(String path) {
		this.path = path;
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
	
	public Signal getLatest(long tick) {
		update(tick);
		return Signal.copy(latest);
	}
	
	private void update(long tick) {
		if (tick >= lastTick) {
			latest = parseCsv();
			lastTick = tick;
		}
	}

	private Signal parseCsv() {
	    try {
	    	String [] nextLine  = reader.readNext();
	    	if (nextLine != null) {
			    return new Signal(12345, Double.parseDouble(nextLine[1]));
			}
	    	else {
	    		 return Signal.none;
	    	}
		} catch (IOException e) {
			e.printStackTrace();
			return Signal.none;
		}
	}
}
