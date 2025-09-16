package com.zenika.tech.lab.ingester.indicators.github.graphql.entities.issues;

import com.zenika.tech.lab.ingester.indicators.github.graphql.entities.TodayCount;

public record RepositoryWithIssueCountToday(IssuesToday issues) implements TodayCount {
    @Override
    public int getCount() {
        return issues.totalCount();
    }
}
