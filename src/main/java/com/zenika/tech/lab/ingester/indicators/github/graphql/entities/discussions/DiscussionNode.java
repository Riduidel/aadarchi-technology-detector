package com.zenika.tech.lab.ingester.indicators.github.graphql.entities.discussions;

import com.zenika.tech.lab.ingester.indicators.github.graphql.entities.Author;

import java.time.OffsetDateTime;

public record DiscussionNode(OffsetDateTime createdAt, Author author) {
}
