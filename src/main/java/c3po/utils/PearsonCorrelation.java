package c3po.utils;

import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import au.com.bytecode.opencsv.CSVReader;

/**
 * This class tries to correlate different signals with eachother and see if 
 * they can be used to predict the future!
 * 
 * @author Joost Pastoor
 *
 */
public class PearsonCorrelation {
	
	public static void main(String[] args) throws IOException {
		
		CSVReader reader = new CSVReader(new FileReader("resources/correlate_me.csv"));
		
		List<String[]> readAll = reader.readAll();

		// Initialize arrays
		int signalCount = SignalName.values().length;
		Double[][] data = new Double[signalCount][readAll.size()];
		
		// Put the CSV data in the arrays
		for(int line = 0; line < readAll.size(); line++) {
			for(int i = 0; i < signalCount; i++)
			data[i][line] = Double.valueOf(readAll.get(line)[i]); 
		}
		
		// Combinations that we wish to try		
		List<SignalName> tickerProperties = Arrays.asList(new SignalName[] {SignalName.bid, SignalName.ask, SignalName.last});
		List<SignalName> orderbookProperties = Arrays.asList(new SignalName[] {SignalName.volume_bid, SignalName.volume_ask, SignalName.p99_bid, SignalName.p99_ask});
		List<Integer> timeshifts = Arrays.asList(60, 300, 600, 3600);
		for(SignalName signal1 : tickerProperties) {
			for(SignalName signal2 : orderbookProperties) {
				// Dont correlate with self
				if(signal1 == signal2) continue;

				for(Integer timeshift : timeshifts) {
					// Shift the data so that the ticker data is matched with the earlier order book data)
					Double[] range1 = Arrays.copyOfRange(data[signal1.ordinal()], timeshift, data[signal1.ordinal()].length);
					Double[] range2 = Arrays.copyOfRange(data[signal2.ordinal()], 0, data[signal2.ordinal()].length - timeshift);
					
					double pearsonCorrelation = getPearsonCorrelation(range1, range2);
					System.out.println("Correlation between " + signal1 + " and " + signal2 + " (="+ timeshift + "secs) = " + Math.round(pearsonCorrelation*1000.0d)/1000.0d);					
				}
			}
		}
		
		reader.close();
	}
	
	public static double getPearsonCorrelation(Double[] scores1, Double[] scores2) {
		double result = 0;
		double sum_sq_x = 0;
		double sum_sq_y = 0;
		double sum_coproduct = 0;
		double mean_x = scores1[0];
		double mean_y = scores2[0];
		for (int i = 2; i < scores1.length + 1; i += 1) {
			double sweep = Double.valueOf(i - 1) / i;
			double delta_x = scores1[i - 1] - mean_x;
			double delta_y = scores2[i - 1] - mean_y;
			sum_sq_x += delta_x * delta_x * sweep;
			sum_sq_y += delta_y * delta_y * sweep;
			sum_coproduct += delta_x * delta_y * sweep;
			mean_x += delta_x / i;
			mean_y += delta_y / i;
		}
		double pop_sd_x = (double) Math.sqrt(sum_sq_x / scores1.length);
		double pop_sd_y = (double) Math.sqrt(sum_sq_y / scores1.length);
		double cov_x_y = sum_coproduct / scores1.length;
		result = cov_x_y / (pop_sd_x * pop_sd_y);
		return result;
	}
	
	public enum SignalName {
		timestamp_ob, volume_bid, volume_ask, p99_bid, p99_ask, p98_bid, p98_ask, p97_bid, p97_ask, p96_bid, p96_ask, p95_bid, p95_ask, p90_bid, p90_ask, p85_bid, p85_ask, p80_bid, p80_ask, p75_bid, p75_ask, timestamp_ticker, high, last, bid, volume, low, ask;
	}
}
