package c3po;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import au.com.bytecode.opencsv.CSVReader;

public class BitstampTickerCsvSource extends BitstampTickerSource {
	private final String path;
	private CSVReader reader;
	private boolean isEmpty = false;
	
	CircularArrayList<Sample[]> interpolationBuffer;
	
	private final long updateRate = 60000; // The frequency at which new data is polled
	private final long interpolationTime = 120000; // Delay data by two minutes for interpolation
	
	public BitstampTickerCsvSource(String path) {
		super();
		this.path = path;
		
		int bufferLength = (int)(interpolationTime / updateRate + 1);
		interpolationBuffer = new CircularArrayList<Sample[]>(bufferLength);
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
	
	/* Todo
	 * - write csv specific interpolation
	 * - switch entry storate to Sample[]
	 * - Promote interpolation logic to baseclass
	 * - Subclasses just implement poll-and-transform-to-sample[]
	 */
	
	@Override
	public void onNewTick(long tick) {
	    try {
	    	if (!isPrewarmed)
	    		prewarm(tick);
	    	
	    	readToCurrent(tick);
	    	updateOutputs(tick);
	    	
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private boolean isPrewarmed = false;
	
	private void prewarm(long tick) throws IOException {
		if (isPrewarmed)
			return;
		
		// Read until we've filled the buffer with enough samples for our interpolation window
		Sample[] entry = parseCsv(reader.readNext());
		interpolationBuffer.add(entry);
		while (entry[0].timestamp < tick + interpolationTime) {
			entry = parseCsv(reader.readNext());
			interpolationBuffer.add(entry);
		}
		
		isPrewarmed = true;
	}
	
	private void readToCurrent(long tick) throws IOException {
		boolean done = false;
    	while(!done) {
    		Sample[] newest = interpolationBuffer.peek();
    		
    		if (newest[0].timestamp < tick + interpolationTime) {
	    		newest = readNewEntry();
	    		
	    		if (newest == null)
	    			done = true;
	    		else
	    			interpolationBuffer.add(newest);
    		}
    		else {
    			done = true;
    		}
    	}
	}
	
	private Sample[] readNewEntry() {
		String[] newest = null;
		try {
			newest = reader.readNext();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return newest != null ? parseCsv(newest) : null;
	}
	
	private Sample[] parseCsv(String[] data) {
		long timestamp = Long.parseLong(data[0]) * 1000;
		Sample[] entry = new Sample[data.length-1];
		
		for (int i = 0; i < entry.length; i++) {
			entry[i] = new Sample(timestamp, Double.parseDouble(data[i+1]));
		}
		
		return entry;
	}
	
	
	private void updateOutputs(long tick) {
		long delayedTick = tick - interpolationTime;
		
		for (int i = 0; i < interpolationBuffer.size(); i++) {
			Sample[] oldEntry = interpolationBuffer.get(i);
			
			if (oldEntry[0].timestamp >= delayedTick) {
				Sample[] newEntry = interpolationBuffer.get(i+1);
				
				for (int j = 0; j < signals.length; j++) {
					Sample sample = Indicators.lerp(oldEntry[j], newEntry[j], delayedTick);
					signals[j].setSample(sample);
				}
				
				return;
			}
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
}
