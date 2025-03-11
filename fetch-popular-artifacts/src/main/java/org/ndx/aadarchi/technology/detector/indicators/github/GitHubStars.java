package org.ndx.aadarchi.technology.detector.indicators.github;

import java.io.IOException;

import org.apache.camel.Exchange;
import org.apache.camel.builder.endpoint.EndpointRouteBuilder;
import org.apache.camel.builder.endpoint.dsl.DirectEndpointBuilderFactory.DirectEndpointBuilder;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.ndx.aadarchi.technology.detector.indicators.IndicatorComputer;
import org.ndx.aadarchi.technology.detector.model.IndicatorRepository;
import org.ndx.aadarchi.technology.detector.model.Technology;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class GitHubStars extends EndpointRouteBuilder implements IndicatorComputer, GitHubBased {

	private static final String ROUTE_NAME = "compute-github-stars";

	@Inject GitHub github;
	
	@Inject IndicatorRepository indicators;

	@Override
	public void configure() throws Exception {
		from(getFromRoute())
			.routeId(ROUTE_NAME)
			.choice()
				.when(this::usesGitHubRepository)
				.process(this::computeGitHubStars)
			.end()
			;
	}

	private DirectEndpointBuilder getFromRoute() {
		return direct(ROUTE_NAME);
	}
	
	@Override
	public String getFromRouteName() {
		return getFromRoute().getUri();
	}

	private void computeGitHubStars(Exchange exchange) throws IOException {
		computeGitHubStars(exchange.getMessage().getBody(Technology.class));
	}

	private void computeGitHubStars(Technology technology) throws IOException {
		
		if(!indicators.hasIndicatorForThisMonth(technology, getIndicatorIdentifier()) ) {
			getGHRepository(technology).ifPresent(repository -> {
				int starsNow = repository.getStargazersCount();
				indicators.saveThisMonth(technology, getIndicatorIdentifier(), Integer.toString(starsNow));
			});
		}
	}

	@Override
	public GitHub getGitHub() {
		return github;
	}

	@Override
	public String getIndicatorIdentifier() {
		return "github.stars";
	}
}
