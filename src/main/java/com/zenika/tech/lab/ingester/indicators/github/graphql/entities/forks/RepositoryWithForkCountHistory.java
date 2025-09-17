package com.zenika.tech.lab.ingester.indicators.github.graphql.entities.forks;

import com.zenika.tech.lab.ingester.indicators.github.graphql.entities.PageableHistory;

public record RepositoryWithForkCountHistory(Forks forks) implements PageableHistory {

	@Override
	public boolean hasPreviousPage() {
		return forks.pageInfo().hasPreviousPage();
	}

	@Override
	public String startCursor() {
		return forks.pageInfo().startCursor();
	}

	@Override
	public boolean hasNoData() {
		return forks == null || forks.pageInfo() == null;
	}
}
