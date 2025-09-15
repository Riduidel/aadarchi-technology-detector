package com.zenika.tech.lab.ingester.indicators.github.graphql;

import java.util.List;

import io.github.bucket4j.Bucket;
import io.smallrye.graphql.client.GraphQLError;

public class GitHubGraphqlException extends RuntimeException {

	private List<GraphQLError> errors;

	public GitHubGraphqlException(String message, List<GraphQLError> errors) {
		super(message);
		this.errors = errors;
	}
}
