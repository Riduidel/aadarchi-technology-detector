package com.zenika.tech.lab.ingester.indicators.github.graphql.entities.stargazer;

import com.zenika.tech.lab.ingester.indicators.github.graphql.User;
import jakarta.json.bind.annotation.JsonbDateFormat;

import java.util.Date;

public record StargazerEvent(@JsonbDateFormat(locale = "en-US", value = "yyyy-MM-dd'T'HH:mm:ss'Z'")Date starredAt, User node) {
}
