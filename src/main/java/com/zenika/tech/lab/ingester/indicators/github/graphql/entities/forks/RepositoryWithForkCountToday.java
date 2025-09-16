package com.zenika.tech.lab.ingester.indicators.github.graphql.entities.forks;

import com.zenika.tech.lab.ingester.indicators.github.graphql.entities.TodayCount;

public record RepositoryWithForkCountToday(int forkCount) implements TodayCount {
    @Override
    public int getCount() {
        return forkCount;
    }
}
