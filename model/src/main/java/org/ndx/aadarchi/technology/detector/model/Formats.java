package org.ndx.aadarchi.technology.detector.model;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Locale;

public class Formats {

	public static DateTimeFormatter MVN_DATE_FORMAT_WITH_DAY =
	new DateTimeFormatterBuilder()
		.appendPattern("MMM dd, yyyy")
		.parseCaseInsensitive()
		.toFormatter(Locale.ENGLISH)
		;
	public static DateTimeFormatter MVN_DATE_FORMAT_WITH_MONTH_ONLY =
	new DateTimeFormatterBuilder()
		.appendPattern("MMM, yyyy")
	 	.parseDefaulting(ChronoField.DAY_OF_MONTH, 1)
		.parseCaseInsensitive()
		.toFormatter(Locale.ENGLISH)
		;
	public static DateTimeFormatter MVN_DATE_FORMAT =
	new DateTimeFormatterBuilder()
		.appendOptional(MVN_DATE_FORMAT_WITH_DAY)
		.appendOptional(MVN_DATE_FORMAT_WITH_MONTH_ONLY)
		.parseCaseInsensitive()
		.toFormatter(Locale.ENGLISH)
		;
	public static DateTimeFormatter INTERNET_ARCHIVE_DATE_FORMAT =
	DateTimeFormatter.ofPattern("uuuuMMddHHmmss", Locale.US)
		.withZone(ZoneId.from(ZoneOffset.UTC))
		;

}
