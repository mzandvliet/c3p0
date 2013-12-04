package c3po;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

public class Time {
	public static final long SECONDS = 1000;
	public static final long MINUTES = SECONDS * 60;
	public static final long HOURS = MINUTES * 60;
	public static final long DAYS = HOURS * 24;
	
	private static final int dateStyle = DateFormat.DEFAULT;
	
	private static final DateFormat dateFormatter = DateFormat.getTimeInstance(dateStyle, new Locale("nl", "NL"));
	
	public static String format(long timestamp) {
		return format(new Date(timestamp));
	}
	
	public static String format(Date date) {
		return dateFormatter.format(date);
	}
}
