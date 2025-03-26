package org.ndx.aadarchi.technology.detector.indicators.github.graphql;

import java.util.List;

import org.ndx.aadarchi.technology.detector.indicators.github.stars.Stargazer;

public class StargazersPage {
	public PageInfo pageInfo;
	public List<StargazerEvent> edges;
}
