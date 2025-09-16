package com.zenika.tech.lab.ingester.indicators.github.graphql.entities.stargazer;

import com.zenika.tech.lab.ingester.indicators.github.graphql.entities.PageableHistory;

public record RepositoryWithStargazerCountHistory(Stargazers stargazers) implements PageableHistory {

    @Override
    public boolean hasPreviousPage() {
        return stargazers.pageInfo().hasPreviousPage();
    }

    @Override
    public String startCursor() {
        return stargazers.pageInfo().startCursor();
    }
}
