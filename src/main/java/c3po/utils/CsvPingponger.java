package c3po.utils;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import c3po.Sample;
import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

public class CsvPingponger {
	
	//================================================================================
    // Program
    //================================================================================
	
	private final static String csvSourcePath = "resources/bitstamp_ticker_till_20131117.csv";
	private final static String csvResultPath = "resources/bitstamp_ticker_till_20131117_pingpong.csv";
	private final static int numOscillations = 2;
			
	public static void main(String[] args) {
		CsvPingponger ponger = new CsvPingponger(csvSourcePath, csvResultPath);
		ponger.open();
		try {
			ponger.pingPongWrite(numOscillations);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ponger.close();
	}
	
	
	//================================================================================
    // Class
    //================================================================================
	
	private final String path;
	private final String resultPath;
	
	private CSVReader reader;
	private CSVWriter writer;
	
	

	public CsvPingponger(String path, String resultPath) {
		super();
		this.path = path;
		this.resultPath = resultPath;
	}
	
	public void open() {
		try {
			reader = new CSVReader(new FileReader(path));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			writer = new CSVWriter(new FileWriter(resultPath));
		} catch (IOException e) {
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
		try {
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void pingPongWrite(int numOscillations) throws IOException {
		final List<String[]> lines = reader.readAll();
		
		long startTime = Long.parseLong(lines.get(0)[0]);
		long endTime = Long.parseLong(lines.get(lines.size()-1)[0]);
		long deltaTime = endTime - startTime;
		long timeStep = deltaTime / lines.size();
		
		for (int i = 0; i < numOscillations; i++) {
			
			// Write regular order
			for (int j = 0; j < lines.size(); j++) {
				String[] sourceLine = lines.get(j);
				
				String[] newLine = new String[sourceLine.length];
				newLine[0] = "" + (startTime + timeStep * j);
				
				for (int k = 1; k < sourceLine.length; k ++) {
					newLine[k] = sourceLine[k];
				}
				
				writer.writeNext(newLine);
			}
			
			// Write reversed
			for (int j = 0; j < lines.size(); j++) {
				int lineIndex = lines.size() - 1 - j;
				String[] sourceLine = lines.get(lineIndex);
				
				String[] newLine = new String[sourceLine.length];
				newLine[0] = "" + (startTime + timeStep * j);
					
				for (int k = 1; k < sourceLine.length; k ++) {
					newLine[k] = sourceLine[k];
				}
				
				writer.writeNext(newLine);
			}
		}
	}
	
}
