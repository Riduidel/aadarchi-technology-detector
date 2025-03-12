package org.ndx.aadarchi.technology.detector.indicators.github;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.StreamSupport;

import org.apache.camel.Exchange;
import org.apache.camel.builder.endpoint.EndpointRouteBuilder;
import org.apache.camel.builder.endpoint.dsl.DirectEndpointBuilderFactory.DirectEndpointBuilder;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHStargazer;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.PagedIterable;
import org.ndx.aadarchi.technology.detector.indicators.IndicatorComputer;
import org.ndx.aadarchi.technology.detector.model.IndicatorNamed;
import org.ndx.aadarchi.technology.detector.model.IndicatorRepository;
import org.ndx.aadarchi.technology.detector.model.IndicatorRepositoryFacade;
import org.ndx.aadarchi.technology.detector.model.Technology;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class GitHubStars extends EndpointRouteBuilder implements IndicatorComputer, GitHubBased {

	private static final String GITHUB_STARS = "github.stars";

	private static final String ROUTE_NAME = "compute-"+GITHUB_STARS.replace('.', '-');

	@Inject GitHub github;
	
	@Inject @IndicatorNamed(GITHUB_STARS) IndicatorRepositoryFacade indicators;
	@Inject StargazerRepository stargazersRepository;

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
		computePastGitHubStars(technology);
		computeStarsToday(technology);
	}

	private void computePastGitHubStars(Technology technology) throws IOException {
		Instant dayBefore = IndicatorRepository.atStartOfMonth()
				.toInstant()
				.minus(1, ChronoUnit.DAYS);
		LocalDate monthBefore = LocalDate.ofInstant(dayBefore, ZoneId.systemDefault())
				.with(TemporalAdjusters.firstDayOfMonth());
		Date startOfPreviousMonth = Date.from(monthBefore.atStartOfDay(ZoneId.systemDefault()).toInstant());
		if(!indicators.hasIndicatorForMonth(technology, startOfPreviousMonth)) {
			getGHRepository(technology).ifPresent(repository -> {
				doLoadAllPastStars(technology, repository);
			});
			// And now, group per month, and this is simply a crazy insert
		}
	}

	private void doLoadAllPastStars(Technology technology, GHRepository repository) {
		Log.infof("Fetching stargazers history of %s", repository.getFullName());
		int total = repository.getStargazersCount();
		PagedIterable<GHStargazer> stargazers = repository
				.listStargazers2()
				.withPageSize(100);
		AtomicInteger atomic = new AtomicInteger(0);
		StreamSupport.stream(stargazers.spliterator(), false)
				.peek(consumer -> {
					int current = atomic.incrementAndGet();
					if(current%100==0) {
						Log.infof("Fetched %d/%d stargazers of %s", 
								current, total, repository.getFullName());
					}
				})
				.forEach(stargazer -> stargazersRepository.persist(technology, stargazer));
	}

	private void computeStarsToday(Technology technology) throws IOException {
		Date startOfMonth = IndicatorRepository.atStartOfMonth();
		if(!indicators.hasIndicatorForMonth(technology, startOfMonth) ) {
			getGHRepository(technology).ifPresent(repository -> {
				int starsNow = repository.getStargazersCount();
				indicators.saveIndicator(technology, Integer.toString(starsNow));
			});
		}
	}

	@Override
	public GitHub getGitHub() {
		return github;
	}
}
