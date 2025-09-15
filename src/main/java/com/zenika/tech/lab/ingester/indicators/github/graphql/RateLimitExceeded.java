package com.zenika.tech.lab.ingester.indicators.github.graphql;

import java.util.List;

import com.zenika.tech.lab.ingester.indicators.github.graphql.GitHubGraphqlFacade.RateLimitMetadata;

import io.smallrye.graphql.client.GraphQLError;

public class RateLimitExceeded extends GitHubGraphqlException {

	public final RateLimitMetadata metadata;

	public RateLimitExceeded(String message, List<GraphQLError> errors, RateLimitMetadata rateLimitMetadata) {
		super(message, errors);
		this.metadata = rateLimitMetadata;
	}

}
