package c3po;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import au.com.bytecode.opencsv.CSVReader;

public class BitstampTickerCsvSource extends BitstampTickerSource {
	private final String path;
	private CSVReader reader;
	private boolean isEmpty = false;
	
	public BitstampTickerCsvSource(long interpolationTime, String path) {
		super(interpolationTime);
		this.path = path;
	}
	
	public void open() {
		try {
			reader = new CSVReader(new FileReader(path));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
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
	protected void pollServer(long clientTimestamp) {
	    try {
	    	readToCurrent(clientTimestamp);	    	
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void readToCurrent(long clientTimestamp) throws IOException {
		
		// Read from the server data up to the end of the interpolation time
		
		long serverTimestamp = clientTimestamp + interpolationTime;
		
		boolean upToDate = false;
    	while(!upToDate) {
    		ServerSampleEntry newest = buffer.peek();
    		
    		if (newest == null || newest.timestamp < serverTimestamp) {
	    		newest = tryGetNewEntry();
	    		
	    		if (newest == null) // Todo: This means the simulation is over, really
	    			upToDate = true;
	    		else
	    			buffer.offer(newest);
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
