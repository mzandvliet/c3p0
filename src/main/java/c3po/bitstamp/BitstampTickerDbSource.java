package c3po.bitstamp;

import java.sql.*;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import c3po.*;
import c3po.db.DbTimeseriesSource;

/**
 * Notice: Currently untested because we do not use it in our current setup.
 */
public class BitstampTickerDbSource extends BitstampTickerSource {
	private static final Logger LOGGER = LoggerFactory.getLogger(BitstampTickerDbSource.class);
	private static final int MAXRETRIES = 3;

	private DbConnection connection = null;
	private DbTimeseriesSource source;
	
	public BitstampTickerDbSource(long timestep, long interpolationTime, DbConnection connection) throws ClassNotFoundException, SQLException {
		  super(timestep, interpolationTime);
		  this.connection = connection;
		  
		  this.source = new DbTimeseriesSource(timestep, connection, "bitstamp_ticker", Arrays.asList("last", "high", "low", "volume", "bid", "ask"));
	}
	
	@Override
	protected void pollServer(long clientTimestamp) {
    	readToCurrent(clientTimestamp);	    	
	}
	
	private void readToCurrent(long clientTimestamp) {
		long serverTimestamp = clientTimestamp + interpolationTime;

		// Fetch the lastest time in buffer
		ServerSampleEntry newest = buffer.peek();
		long newestTimeInBuffer = newest != null ? newest.timestamp : clientTimestamp;
		source.setLatestSampleTimestamp(newestTimeInBuffer);
		
		// See if there are new samples from after that moment
		source.getNewSamples(serverTimestamp);
	}
	
	
	@Override
	public boolean isEmpty() {
		return false;
	}

	@Override
	public boolean open() {
		return connection.open();
	}

	@Override
	public boolean close() {
		return connection.close();
	}
}
