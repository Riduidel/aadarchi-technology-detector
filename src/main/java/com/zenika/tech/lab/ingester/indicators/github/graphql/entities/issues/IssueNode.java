package com.zenika.tech.lab.ingester.indicators.github.graphql.entities.issues;

import com.zenika.tech.lab.ingester.indicators.github.graphql.entities.Author;

import java.time.OffsetDateTime;

public record IssueNode(OffsetDateTime createdAt, Author author) {
}
