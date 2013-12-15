package c3po.utils;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import au.com.bytecode.opencsv.CSVReader;

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
		List<SignalName> correlationProperties = Arrays.asList(new SignalName[] {SignalName.volume_bid, SignalName.volume_ask, SignalName.p99_bid});
		
		for(SignalName signal1 : correlationProperties) {
			for(SignalName signal2 : correlationProperties) {
				// Dont correlate with self
				if(signal1 == signal2) continue;
				
				double pearsonCorrelation = getPearsonCorrelation(data[signal1.ordinal()], data[signal2.ordinal()]);
				System.out.println("Correlation between " + signal1 + " and " + signal2 + " = " + pearsonCorrelation);
			}
		}

		
		double pearsonCorrelation = getPearsonCorrelation(data[SignalName.last.ordinal()], data[SignalName.bid.ordinal()]);
		System.out.println(pearsonCorrelation);
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
