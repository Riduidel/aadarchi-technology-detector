package com.zenika.tech.lab.ingester.indicators.github.graphql.entities.stargazer;

import com.zenika.tech.lab.ingester.indicators.github.graphql.entities.PageInfo;

import java.util.List;

public record Stargazers(List<StargazerEvent> edges, PageInfo pageInfo) {
}
