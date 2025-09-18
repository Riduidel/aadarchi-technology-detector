package com.zenika.tech.lab.ingester.indicators.github.graphql.entities.forks;

import com.zenika.tech.lab.ingester.indicators.github.graphql.entities.PageInfo;

import java.util.List;

public record Forks(Integer totalCount, List<ForkNode> nodes, PageInfo pageInfo) {
}
