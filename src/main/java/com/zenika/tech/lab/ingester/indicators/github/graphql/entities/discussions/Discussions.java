package com.zenika.tech.lab.ingester.indicators.github.graphql.entities.discussions;

import com.zenika.tech.lab.ingester.indicators.github.graphql.entities.PageInfo;

import java.util.List;

public record Discussions(List<DiscussionNode> nodes, PageInfo pageInfo) {
}
