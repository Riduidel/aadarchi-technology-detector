package com.zenika.tech.lab.ingester.indicators.github.graphql.entities.issues;


import com.zenika.tech.lab.ingester.indicators.github.graphql.entities.PageableHistory;

public record RepositoryWithIssueCountHistory(Issues issues) implements PageableHistory {

    @Override
    public boolean hasPreviousPage() {
        return issues.pageInfo().hasPreviousPage();
    }

    @Override
    public String startCursor() {
        return issues.pageInfo().startCursor();
    }

    @Override
    public boolean hasNoData() {
        return issues == null || issues.pageInfo() == null;
    }
}
