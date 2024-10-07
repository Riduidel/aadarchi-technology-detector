package org.ndx.aadarchi.technology.detector.model;

import java.util.Optional;

import org.jilt.Builder;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Builder(toBuilder = "toBuilder")
public class GitHubDetails {
	private String path;
	private Optional<Integer> stargazers = Optional.empty();
	@JsonCreator
	public GitHubDetails(@JsonProperty("path") String path, @JsonProperty("stargazers") Optional<Integer> stargazers) {
		super();
		this.path = path;
		this.stargazers = stargazers;
	}
	public GitHubDetails(String path, int stargazers) {
		super();
		this.path = path;
		if(stargazers!=0)
			this.stargazers = Optional.of(stargazers);
	}
	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}
	public Optional<Integer> getStargazers() {
		return stargazers;
	}
	public void setStargazers(Optional<Integer> stargazers) {
		this.stargazers = stargazers;
	}
}
