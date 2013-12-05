package c3po.bitstamp;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import c3po.Sample;
import au.com.bytecode.opencsv.CSVReader;

public class BitstampSimulationTickerCsvSource extends BitstampTickerSource {
	private final String path;
	private CSVReader reader;
	private boolean isEmpty = false;
	
	public BitstampSimulationTickerCsvSource(long timestep, long interpolationTime, String path) {
		super(timestep, interpolationTime);
		this.path = path;
	}
	
	public boolean open() {
		try {
			reader = new CSVReader(new FileReader(path));
			return true;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}
	
	public boolean close() {
		try {
			reader.close();
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
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
		
		// Todo: REMOVE THE CLAMPSSSS, FIX THE CSVSSSSSSSSS!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
		
		entry.set(SignalName.LAST.ordinal(), new Sample(serverTimestamp, Math.max(Double.parseDouble(data[2]),100d)));
		entry.set(SignalName.HIGH.ordinal(), new Sample(serverTimestamp, Math.max(Double.parseDouble(data[1]),100d)));
		entry.set(SignalName.LOW.ordinal(), new Sample(serverTimestamp, Math.max(Double.parseDouble(data[5]),100d)));
		entry.set(SignalName.VOLUME.ordinal(), new Sample(serverTimestamp, Math.max(Double.parseDouble(data[4]),100d)));
		entry.set(SignalName.BID.ordinal(), new Sample(serverTimestamp, Math.max(Double.parseDouble(data[3]),100d)));
		entry.set(SignalName.ASK.ordinal(), new Sample(serverTimestamp, Math.max(Double.parseDouble(data[6]),100d)));
		
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
