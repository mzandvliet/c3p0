package c3po.utils;

import java.io.IOException;

public class MarketCrashGenerator {

	//"1385156429","790.00","769.97","769.35","25376.2602582400","683.20","769.97";
	private final static long lastTimestamp = 1385156429;
	private final static double lastHigh = 790.00;
	private final static double lastLast = 769.97;
	private final static double lastBid = 769.35;
	private final static double lastVolume = 25376.2602582400;
	private final static double lastLow = 683.20;
	private final static double lastAsk = 769.97;
	
	private final static double totalTimeInMins = 600;
	private final static double crashAfterMins = 60;
	private final static double crashDuration = 240;
	private final static double startOfCrashValuePercent = 0.75;
	private final static double endOfCrashValuePercent = 0.10;
	
	public static void main(String[] args) throws IOException {

		
		double currentHigh = lastHigh;
		double currentLast = lastLast;
		double currentBid = lastBid;
		double currentVolume = lastVolume;
		double currentLow = lastLow;
		double currentAsk = lastAsk;
		
		long currentTimestamp = lastTimestamp;
		
		// The loop
		for(int t = 0; t < totalTimeInMins; t++) {
			currentTimestamp += 60;
			
			if(t < crashAfterMins) {
				currentHigh -= (lastHigh * (1-startOfCrashValuePercent) / (double) crashAfterMins);		
				currentLast -= (lastLast * (1-startOfCrashValuePercent) / (double) crashAfterMins);		
				currentBid -= (lastBid * (1-startOfCrashValuePercent) / (double) crashAfterMins);			
				currentVolume -= (lastVolume * (1-startOfCrashValuePercent) / (double) crashAfterMins);		
				currentLow -= (lastLow * (1-startOfCrashValuePercent) / (double) crashAfterMins);		
				currentAsk -= (lastAsk * (1-startOfCrashValuePercent) / (double) crashAfterMins);		
			}
			
			if(t >= crashAfterMins + crashDuration) {
				currentHigh = lastHigh * endOfCrashValuePercent + Math.random();
				currentLast = lastLast * endOfCrashValuePercent + Math.random();
				currentBid = lastBid * endOfCrashValuePercent + Math.random();
				currentVolume = lastVolume * endOfCrashValuePercent + Math.random();
				currentLow = lastLow * endOfCrashValuePercent + Math.random();
				currentAsk = currentAsk * endOfCrashValuePercent + Math.random();
			}
		
			// "1384079023","363.65","305.05","303.99","60752.6095822100","267.21","305.05"
			System.out.println("\""+currentTimestamp+"\",\""+currentHigh+"\",\""+currentLast+"\",\""+currentBid+"\",\""+currentVolume+"\",\""+currentLow+"\",\""+currentAsk+"\"");
		}
	
	}

}
