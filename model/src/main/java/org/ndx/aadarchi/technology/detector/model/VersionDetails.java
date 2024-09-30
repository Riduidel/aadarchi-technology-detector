package org.ndx.aadarchi.technology.detector.model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import org.jilt.Builder;

@Builder(toBuilder = "toBuilder")
public class VersionDetails {
	private String usages;
	private String date;
	private int users;
	private int previousUsers;
	private int interpolatedUsers;
	@Builder.Ignore
	private transient Optional<LocalDate> parsedDate;
	public VersionDetails() {
		
	}
	public VersionDetails(String date) {
		super();
		this.date = date;
	}
	public VersionDetails(String usages, String date, int users, int previousUsers, int interpolatedUsers) {
		super();
		this.usages = usages;
		this.date = date;
		this.users = users;
		this.previousUsers = previousUsers;
		this.interpolatedUsers = interpolatedUsers;
	}
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
	public String getUsages() {
		return usages;
	}
	public void setUsages(String usages) {
		this.usages = usages;
	}
	public String getDate() {
		return date;
	}
	public void setDate(String date) {
		this.date = date;
	}
	public int getUsers() {
		return users;
	}
	public void setUsers(int users) {
		this.users = users;
	}
	public int getPreviousUsers() {
		return previousUsers;
	}
	public void setPreviousUsers(int previousUsers) {
		this.previousUsers = previousUsers;
	}
	public int getInterpolatedUsers() {
		return interpolatedUsers;
	}
	public void setInterpolatedUsers(int interpolatedUsers) {
		this.interpolatedUsers = interpolatedUsers;
	}
}