package com.zenika.tech.lab.ingester.indicators.github.graphql.entities.issues;

import com.zenika.tech.lab.ingester.indicators.github.graphql.entities.PageInfo;

import java.util.List;

public record Issues(List<IssueNode> nodes, PageInfo pageInfo) {
}
