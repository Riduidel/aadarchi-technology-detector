package org.ndx.aadarchi.technology.detector.indicators.github.graphql.entities;

import java.util.Date;
import java.util.List;

import org.ndx.aadarchi.technology.detector.indicators.github.graphql.User;

import jakarta.json.bind.annotation.JsonbDateFormat;

public class RepositoryWithStargazerList {
	public static class PageOfStargazers {
		public PageInfo pageInfo;
		public List<StargazerEvent> edges;
	}
	public class StargazerEvent {
		@JsonbDateFormat(locale = "en-US", value = "yyyy-MM-dd'T'HH:mm:ss'Z'")
		public Date starredAt;
		public User node;
	}

	public PageOfStargazers stargazers;
}
