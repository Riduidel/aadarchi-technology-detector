package org.ndx.aadarchi.technology.detector.augmenters.github;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Stargazer implements Comparable<Stargazer>{

	private Date starredAt;
	private String user;

	@JsonCreator
	public Stargazer(@JsonProperty("starredAt") Date starredAt, @JsonProperty("user") String user) {
		this.starredAt = starredAt;
		this.user = user;
	}
	@Override
	public int compareTo(Stargazer o) {
		int returned = 0;
		if(returned==0)
			returned = starredAt.compareTo(o.starredAt);
		if(returned==0)
			returned = user.compareTo(o.user);
		return returned;
	}

	public Date getStarredAt() {
		return starredAt;
	}

	public String getUser() {
		return user;
	}
	
}