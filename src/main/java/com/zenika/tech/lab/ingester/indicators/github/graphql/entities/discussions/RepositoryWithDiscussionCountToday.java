package com.zenika.tech.lab.ingester.indicators.github.graphql.entities.discussions;

import com.zenika.tech.lab.ingester.indicators.github.graphql.entities.TodayCount;

public record RepositoryWithDiscussionCountToday(DiscussionsToday discussions) implements TodayCount {
    @Override
    public int getCount() {
        return discussions.totalCount();
    }
}
