package com.coresystems.sparky;

/**
 * Just a quick way to log stuff to the console - we can add log4j later or what
 * have you.
 * 
 * @author zafr
 *
 */
public final class Log {
	private static final String LOG_FORMAT = "[%s] : %s - %s";
	private static final String EMPTY_STRING = "";

	private Log() {

	}

	public static void e(String tag, String message, Throwable exception) {
		print("E", tag, message);
		if (exception != null) {
			exception.printStackTrace();
		}
	}

	public static void w(String tag, String message) {
		print("W", tag, message);
	}

	public static void i(String tag, String message) {
		print("I", tag, message);
	}

	private static void print(String logLevel, String tag, String message) {
		System.out.println(String.format(LOG_FORMAT, logLevel != null ? logLevel : EMPTY_STRING,
				tag != null ? tag : EMPTY_STRING, message != null ? message : EMPTY_STRING));

	}
}
