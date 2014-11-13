package com.am.jlfu.fileuploader.utils;


/**
 * Provdes static unit conversion methods.<br>
 * A javascript method is also contained in <code>javalargefileuploader.js</code>
 * 
 * @author antoinem
 */
public final class UnitConverter {

	public static String getFormattedTime(long secs)
	{
		if (secs < 1) {
			return "-";
		}

		double hours = Math.floor(secs / (60 * 60));

		double divisor_for_minutes = secs % (60 * 60);
		double minutes = Math.floor(divisor_for_minutes / 60);

		double divisor_for_seconds = divisor_for_minutes % 60;
		double seconds = Math.ceil(divisor_for_seconds);

		String returned = "";
		boolean displaySeconds = true;
		if (hours > 0) {
			returned += ((int)hours) + "h";
			displaySeconds = false;
		}
		if (minutes > 0) {
			returned += ((int)minutes) + "m";
			displaySeconds &= minutes <= 10;
		}
		if (displaySeconds) {
			returned += ((int)seconds) + "s";
		}
		return returned;
	}


	public static  String getFormattedSize(long size) {
		if (size < 1024) {
			return format(size) + "B";
		}
		else if (size < 1048576) {
			return format(size / 1024f) + "KB";
		}
		else if (size < 1073741824) {
			return format(size / 1048576f) + "MB";
		}
		else if (size < 1099511627776l) {
			return format(size / 1073741824f) + "GB";
		}
		else if (size < 1125899906842624l) {
			return format(size / 1099511627776f) + "TB";
		}
		return null;
	}


	public static float format(float f) {
		return ((float)Math.ceil(f * 100)) / 100f;
	}


}
