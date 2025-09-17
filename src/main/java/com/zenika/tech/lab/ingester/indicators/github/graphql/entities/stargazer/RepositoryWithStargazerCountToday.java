package com.zenika.tech.lab.ingester.indicators.github.graphql.entities.stargazer;

import com.zenika.tech.lab.ingester.indicators.github.graphql.entities.TodayCount;

public record RepositoryWithStargazerCountToday(int stargazerCount) implements TodayCount {
	@Override
	public int getCount() {
		return stargazerCount;
	}
}
