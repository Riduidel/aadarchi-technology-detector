package org.ndx.aadarchi.technology.detector.model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class VersionDetails {
	String usages;
	String date;
	int users;
	int previousUsers;
	int interpolatedUsers;
	transient Optional<LocalDate> parsedDate;
	public LocalDate getParsedDate(DateTimeFormatter format) {
		if(parsedDate==null) {
			String parsableDate = date.trim();
			if(parsableDate.startsWith("(")) {
				parsableDate = parsableDate.substring(1, parsableDate.length()-1);
			}
			parsedDate = Optional.ofNullable(LocalDate.parse(parsableDate, format));
		}
		return parsedDate.get();
	}
}