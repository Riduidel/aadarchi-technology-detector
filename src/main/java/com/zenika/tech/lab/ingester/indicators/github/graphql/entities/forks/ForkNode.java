package com.zenika.tech.lab.ingester.indicators.github.graphql.entities.forks;

import com.zenika.tech.lab.ingester.indicators.github.graphql.entities.Owner;

import java.time.OffsetDateTime;

public record ForkNode(OffsetDateTime createdAt, Owner owner) {
}
