package c3po;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import au.com.bytecode.opencsv.CSVReader;

/* Todo:
 * - Move interpolation to base class
 * - Implement server timeout strategy (extrapolation for a little while, then crisis mode)
 * - Implement high frequency polling to avoid server update misses
 */
public class BitstampTickerCsvSource extends BitstampTickerSource {
	private final String path;
	private CSVReader reader;
	private boolean isEmpty = false;
	
	CircularArrayList<ServerSampleEntry> buffer;
	
	private final long updateRate = 60000; // The frequency at which new data is polled, (todo: fix polling misses)
	
	public BitstampTickerCsvSource(long interpolationTime, String path) {
		super(interpolationTime);
		this.path = path;
		
		int bufferLength = (int)(interpolationTime / updateRate + 1);
		buffer = new CircularArrayList<ServerSampleEntry>(bufferLength);
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
	public void onNewTick(long clientTimestamp) {
	    try {
	    	if (!isPrewarmed)
	    		prewarm(clientTimestamp);
	    	
	    	readToCurrent(clientTimestamp);
	    	updateOutputs(clientTimestamp);
	    	
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private boolean isPrewarmed = false;
	
	private void prewarm(long clientTimestamp) throws IOException {
		if (isPrewarmed)
			return;
		
		// Read until we've filled the buffer with enough samples from the simulated server for our interpolation window
		
		long serverTimestamp = clientTimestamp + interpolationTime;
		
		ServerSampleEntry entry = parseCsv(reader.readNext());
		buffer.add(entry);
		while (entry.timestamp <= serverTimestamp) {
			entry = parseCsv(reader.readNext());
			buffer.add(entry);
		}
		
		isPrewarmed = true;
	}
	
	private void readToCurrent(long clientTimestamp) throws IOException {
		
		// Read to the most up-to-date server entry we can get
		
		long serverTimestamp = clientTimestamp + interpolationTime;
		
		boolean upToDate = false;
    	while(!upToDate) {
    		ServerSampleEntry newest = buffer.peek();
    		
    		if (newest.timestamp < serverTimestamp) {
	    		newest = tryGetNewEntry();
	    		
	    		if (newest == null) // Todo: This means the simulation is over, really
	    			upToDate = true;
	    		else
	    			buffer.add(newest);
    		}
    		else {
    			upToDate = true;
    		}
    	}
	}
	
	private ServerSampleEntry tryGetNewEntry() {
		String[] newestData = null;
		
		try {
			newestData = reader.readNext();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return newestData != null ? parseCsv(newestData) : null;
	}
	
	private ServerSampleEntry parseCsv(String[] data) {
		long serverTimestamp = Long.parseLong(data[0]) * 1000;
		
		ServerSampleEntry entry = new ServerSampleEntry(serverTimestamp, data.length-1);
		
		for (int i = 0; i < entry.size(); i++) {
			entry.set(i, new Sample(serverTimestamp, Double.parseDouble(data[i+1])));
		}
		
		return entry;
	}
	
	private void updateOutputs(long clientTimestamp) {
		
		/*
		 *  If clientTime is older than most recent server entry (which happens at
		 *   startup), just return the oldest possible value. This results in a
		 *   constant signal until server start time is reached.
		 */
		if (clientTimestamp <= buffer.get(0).timestamp) {
			ServerSampleEntry oldEntry = buffer.get(0);
			
			for (int j = 0; j < signals.length; j++) {
				Sample sample = oldEntry.get(j);
				signals[j].setSample(sample);
			}
		}
		
		/*
		 * Todo:
		 * 
		 * if client time is newer than server time (in case of network error or something) we
		 * should do error handling. Either extrapolate and hope you regain connection or go
		 *  into crisis mode.
		 */
		
		/*
		 *  If client time falls within the buffered entries, interpolate the result
		 */
		for (int i = 0; i < buffer.size(); i++) {
			ServerSampleEntry oldEntry = buffer.get(i);
			
			if (clientTimestamp > oldEntry.timestamp) {
				ServerSampleEntry newEntry = buffer.get(i+1);
				
				for (int j = 0; j < signals.length; j++) {
					Sample sample = Indicators.lerp(oldEntry.get(j), newEntry.get(j), clientTimestamp);
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
	
	public class ServerSampleEntry {
		public final long timestamp;
		public final Sample[] samples;
		
		public ServerSampleEntry(long timestamp, int length) {
			this.timestamp = timestamp;
			this.samples = new Sample[length];
		}
		
		public int size() {
			return samples.length;
		}
		
		public Sample get(int i) {
			return samples[i];
		}
		
		public void set(int i, Sample sample) {
			samples[i] = sample;
		}
	}
}
