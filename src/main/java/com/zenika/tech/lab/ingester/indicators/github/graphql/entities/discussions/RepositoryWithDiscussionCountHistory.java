package com.zenika.tech.lab.ingester.indicators.github.graphql.entities.discussions;

import com.zenika.tech.lab.ingester.indicators.github.graphql.entities.PageableHistory;

public record RepositoryWithDiscussionCountHistory(Discussions discussions) implements PageableHistory {
	@Override
	public boolean hasNoData() {
		return false;
	}

	@Override
	public boolean hasPreviousPage() {
		return false;
	}

	@Override
	public String startCursor() {
		return "";
	}
}
