package org.ndx.aadarchi.technology.detector.indicators.github.graphql;

import java.util.Date;

import jakarta.json.bind.annotation.JsonbDateFormat;

public class StargazerEvent {
	@JsonbDateFormat(locale = "en-US", value = "yyyy-MM-dd'T'HH:mm:ss'Z'")
	public Date starredAt;
	public User node;
}
