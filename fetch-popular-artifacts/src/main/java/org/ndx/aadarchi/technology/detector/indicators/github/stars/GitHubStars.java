package org.ndx.aadarchi.technology.detector.indicators.github.stars;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Date;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.camel.Exchange;
import org.apache.camel.builder.endpoint.EndpointRouteBuilder;
import org.apache.camel.builder.endpoint.dsl.DirectEndpointBuilderFactory.DirectEndpointBuilder;
import org.apache.camel.util.Pair;
import org.ndx.aadarchi.technology.detector.indicators.IndicatorComputer;
import org.ndx.aadarchi.technology.detector.indicators.github.GitHubBased;
import org.ndx.aadarchi.technology.detector.indicators.github.graphql.GitHubGraphqlFacade;
import org.ndx.aadarchi.technology.detector.indicators.github.graphql.StargazerEvent;
import org.ndx.aadarchi.technology.detector.indicators.github.graphql.StargazerListRepository;
import org.ndx.aadarchi.technology.detector.model.IndicatorNamed;
import org.ndx.aadarchi.technology.detector.model.IndicatorRepository;
import org.ndx.aadarchi.technology.detector.model.IndicatorRepositoryFacade;
import org.ndx.aadarchi.technology.detector.model.Technology;

import io.quarkus.logging.Log;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class GitHubStars extends EndpointRouteBuilder implements IndicatorComputer, GitHubBased {

	public static final String GITHUB_STARS = "github.stars";

	private static final String ROUTE_NAME = "compute-"+GITHUB_STARS.replace('.', '-');

	@Inject @IndicatorNamed(GITHUB_STARS) IndicatorRepositoryFacade indicators;
	@Inject StargazerRepository stargazersRepository;
	@Inject GitHubGraphqlFacade githubClient;

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
		computePastStars(technology);
	}

	private void computePastStars(Technology technology) throws IOException {
		Instant dayBefore = IndicatorRepository.atStartOfMonth()
				.toInstant()
				.minus(1, ChronoUnit.DAYS);
		LocalDate monthBefore = LocalDate.ofInstant(dayBefore, ZoneId.systemDefault())
				.with(TemporalAdjusters.firstDayOfMonth());
		Date startOfPreviousMonth = Date.from(monthBefore.atStartOfDay(ZoneId.systemDefault()).toInstant());
		if(!indicators.hasIndicatorForMonth(technology, startOfPreviousMonth)) {
			getRepository(technology).ifPresent(pair -> {
				loadAllPastStargazers(technology, pair);
				computeAllPastStars(technology, pair);
			});
		}
	}

	private void computeAllPastStars(Technology technology, Pair<String> pair) {
		stargazersRepository.groupStarsByMonths(technology, pair).stream()
			.forEach(indicator -> indicators.maybePersist(indicator));
	}

	private void loadAllPastStargazers(Technology technology, Pair<String> path) {
		long localCount = stargazersRepository.count(path);
		int remoteCount = githubClient.getStargazers(path.getLeft(), path.getRight());
		boolean forceRedownload = (remoteCount-localCount)/(remoteCount*1.0)>0.1;
		if(forceRedownload) {
			Log.infof("For %s/%s, we have %d stars locally, and there are %d stars on GitHub. Forcing full redownload", path.getLeft(), path.getRight(), localCount, remoteCount);
		} else {
			Log.infof("For %s/%s, we have %d stars locally, and there are %d stars on GitHub", path.getLeft(), path.getRight(), localCount, remoteCount);
		}
		if(localCount<remoteCount) {
			githubClient.getAllStargazers(path.getLeft(), path.getRight(), forceRedownload,
				repositoryPage -> {
					try {
						return this.processRepositoryPage(path, repositoryPage);
					} finally {
						Log.infof("Written %d/%d stargazers of %s/%s", 
								stargazersRepository.count(path),
								remoteCount,
								path.getLeft(),
								path.getRight());
					}
				});
		}
	}

	/**
	 * Process one page of graphql query result
	 * @param path
	 * @param repositoryPage
	 * @return true if we have to continue the process (in other words, if at least one event was persisted)
	 */
	private boolean processRepositoryPage(Pair<String> path, StargazerListRepository repositoryPage) {
		return repositoryPage.stargazers.edges
			.stream()
			.map(event -> maybePersist(path, repositoryPage, event))
			.collect(Collectors.reducing((a, b) -> a||b))
			.orElse(false);
	}

	/**
	 * Persist event
	 * @param path
	 * @param repositoryPage
	 * @param event
	 * @return true if database changed, false if event already existed in db
	 */
	private boolean maybePersist(Pair<String> path, StargazerListRepository repositoryPage, StargazerEvent event) {
		Stargazer toPersist = new Stargazer(
				path.getLeft(), path.getRight(),
				event.starredAt,
				event.node.login
				);
		
		return stargazersRepository.maybePersist(toPersist);
	}

}
