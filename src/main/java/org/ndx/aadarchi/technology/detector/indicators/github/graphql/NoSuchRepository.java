package org.ndx.aadarchi.technology.detector.indicators.github.graphql;

import java.util.List;

import io.smallrye.graphql.client.GraphQLError;

public class NoSuchRepository extends GitHubGraphqlException {

	public NoSuchRepository(String message, List<GraphQLError> errors) {
		super(message, errors);
	}

}
