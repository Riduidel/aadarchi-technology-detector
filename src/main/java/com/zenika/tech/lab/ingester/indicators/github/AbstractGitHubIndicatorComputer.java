package com.zenika.tech.lab.ingester.indicators.github;

import com.zenika.tech.lab.ingester.indicators.IndicatorComputer;
import com.zenika.tech.lab.ingester.indicators.github.graphql.GitHubGraphqlException;
import com.zenika.tech.lab.ingester.indicators.github.graphql.GitHubGraphqlFacade;
import com.zenika.tech.lab.ingester.indicators.github.graphql.entities.PageableHistory;
import com.zenika.tech.lab.ingester.model.IndicatorRepositoryFacade;
import com.zenika.tech.lab.ingester.model.Technology;
import io.quarkus.logging.Log;
import org.apache.camel.Exchange;
import org.apache.camel.builder.endpoint.dsl.DirectEndpointBuilderFactory.DirectEndpointBuilder;
import org.apache.camel.support.processor.idempotent.MemoryIdempotentRepository;
import org.apache.camel.util.Pair;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

public abstract class AbstractGitHubIndicatorComputer<T, V extends PageableHistory> extends AbstractGitHubEndpointRouteBuilder implements IndicatorComputer, GitHubBased {

	public final String githubIndicatorsName;
	private final String routeName;


	@ConfigProperty(name = "tech-lab-ingester.github.missing-count-percentage-threshold", defaultValue = "10")
	int missingCountPercentageThreshold;
	private final IndicatorRepositoryFacade indicators;
	protected final GitHubGraphqlFacade githubClient;
	private final GithubIndicatorRepository<T> repository;

	protected AbstractGitHubIndicatorComputer(
			IndicatorRepositoryFacade indicators,
			GithubIndicatorRepository<T> repository,
			GitHubGraphqlFacade githubClient,
			String githubIndicatorsName) {

		this.indicators = indicators;
		this.repository = repository;
		this.githubClient = githubClient;
		this.githubIndicatorsName = githubIndicatorsName;

		this.routeName = "compute-" + githubIndicatorsName.replace('.', '-');
	}

	/**
	 * Converts a raw event object retrieved from the GitHub API into a typed entity
	 * that implements {@code T}.
	 *
	 * <p>The implementation is responsible for mapping the low-level API response
	 * (e.g., a GraphQL node) to the corresponding domain entity.</p>
	 *
	 * @param ownerAndRepositoryName a pair containing the repository owner (left)
	 *                               and the repository name (right)
	 * @param rawEvent               the raw event object returned by the API for this repository
	 * @return the mapped entity representing the event
	 */
	protected abstract T toEntity(Pair<String> ownerAndRepositoryName, Object rawEvent);

	/**
	 * Extracts the list of events contained in a single page of the API response.
	 *
	 * <p>This method should unwrap the {@code V} page object and return the
	 * collection of raw events (e.g., nodes or edges) that will later be
	 * transformed into entities.</p>
	 *
	 * @param repositoryPage a single page of results returned by the API
	 * @return the list of raw event objects; must never be {@code null},
	 * use an empty list if no events are present
	 */
	protected abstract List<? extends Object> getEvents(V repositoryPage);

	/**
	 * Retrieves the cumulative total count of this indicator for the given repository,
	 * measured from its creation until the current day.
	 *
	 * <p>This represents the "all-time" or "lifetime" value of the indicator
	 * as observed today.</p>
	 *
	 * @param ownerAndRepositoryName a pair containing the repository owner (left)
	 *                               and the repository name (right)
	 * @return the total count of the indicator as of today
	 */
	protected abstract int getTotalCountAsOfTodayFor(Pair<String> ownerAndRepositoryName);

	/**
	 * Iterates through the historical pages of this indicator for the given repository,
	 * invoking the provided predicate on each page.
	 *
	 * <p>This method allows processing or re-downloading of historical data,
	 * typically using pagination. The {@code processIndicator} predicate can be used
	 * to decide whether to continue processing after each page.</p>
	 *
	 * @param ownerAndRepositoryName a pair containing the repository owner (left)
	 *                               and the repository name (right)
	 * @param forceRedownload        if {@code true}, forces retrieval of all pages even
	 *                               if cached data is available
	 * @param processIndicator       a predicate that is applied to each page; returning
	 *                               {@code true} continues processing, {@code false} stops
	 */
	protected abstract void getHistoryCountFor(Pair<String> ownerAndRepositoryName, boolean forceRedownload, Predicate<V> processIndicator);

	@Override
	public void configure() throws Exception {
		super.configureExceptions();
		from(getFromRoute())
				.routeId(routeName)
				.idempotentConsumer()
				.body(Technology.class, t -> String.format("%s-%s", githubIndicatorsName, t.repositoryUrl))
				.idempotentRepository(MemoryIdempotentRepository.memoryIdempotentRepository(10 * 2))
				.process(this::computeGitHubIndicators)
				.end()
		;
	}

	private DirectEndpointBuilder getFromRoute() {
		return direct(routeName);
	}

	@Override
	public String getFromRouteName() {
		return getFromRoute().getUri();
	}

	private void computeGitHubIndicators(Exchange exchange) throws IOException {
		try {
			computeGitHubIndicators(exchange.getMessage().getBody(Technology.class));
		} catch (GitHubGraphqlException e) {
			Log.warnf(e, "Unable to fetch %s for missing repo", githubIndicatorsName);
		}
	}

	private void computeGitHubIndicators(Technology technology) throws IOException {
		computePastIndicators(technology);
	}

	private void computePastIndicators(Technology technology) throws IOException {
		getRepository(technology).ifPresent(ownerAndRepositoryName -> {
			loadAllPastIndicators(ownerAndRepositoryName);
			computeAllPastIndicators(technology, ownerAndRepositoryName);
		});
	}

	private void loadAllPastIndicators(Pair<String> ownerAndRepositoryName) {
		long localCount = repository.count(ownerAndRepositoryName);
		int remoteCount = getTotalCountAsOfTodayFor(ownerAndRepositoryName);
		int missingCountPercentage = (int) (((remoteCount - localCount) / (remoteCount * 1.0)) * 100.0);
		boolean forceRedownload = missingCountPercentage > missingCountPercentageThreshold;
		if (forceRedownload) {
			Log.infof("📥 For %s/%s, we have %d %s locally, and there are %d %s on GitHub (we lack %d %%). Forcing full redownload", ownerAndRepositoryName.getLeft(), ownerAndRepositoryName.getRight(), localCount, githubIndicatorsName, remoteCount, githubIndicatorsName, missingCountPercentage);
		} else {
			Log.infof("For %s/%s, we have %d %s locally, and there are %d %s on GitHub (we lack %d %%)", ownerAndRepositoryName.getLeft(), ownerAndRepositoryName.getRight(), localCount, githubIndicatorsName, remoteCount, githubIndicatorsName, missingCountPercentage);
		}
		boolean shouldDownloadIndicators = localCount < remoteCount;
		if (shouldDownloadIndicators) {
			AtomicInteger processedCount = new AtomicInteger();
			getHistoryCountFor(ownerAndRepositoryName, forceRedownload, repositoryPage -> {
				try {
					processedCount.addAndGet(getEvents(repositoryPage).size());
					return this.processPage(ownerAndRepositoryName, repositoryPage);
				} finally {
					if (Log.isDebugEnabled()) {
						Log.debugf("Processed %d elements. Written %d/%d %s of %s/%s",
								processedCount.intValue(),
								repository.count(ownerAndRepositoryName),
								remoteCount,
								githubIndicatorsName,
								ownerAndRepositoryName.getLeft(),
								ownerAndRepositoryName.getRight());
					}
				}
			});
		}
	}

	private void computeAllPastIndicators(Technology technology, Pair<String> ownerAndRepositoryName) {
		repository.groupIndicatorsByMonths(technology, ownerAndRepositoryName)
				.forEach(indicators::maybePersist);
	}

	/**
	 * Process one page of graphql query result
	 *
	 * @param ownerAndRepositoryName pair of owner and repository name
	 * @param repositoryPage         page to process
	 * @return true if we have to continue the process (in other words, if at least one event was persisted)
	 */
	private boolean processPage(Pair<String> ownerAndRepositoryName, V repositoryPage) {
		return getEvents(repositoryPage)
				.stream()
				.map(event -> maybePersist(ownerAndRepositoryName, event))
				.reduce((a, b) -> a || b)
				.orElse(false);
	}

	/**
	 * Persist event
	 *
	 * @param ownerAndRepositoryName pair of owner and repository name
	 * @param event                  event to persist
	 * @return true if database changed, false if event already existed in db
	 */
	private boolean maybePersist(Pair<String> ownerAndRepositoryName, Object event) {
		T toPersist = toEntity(ownerAndRepositoryName, event);
		return repository.maybePersist(toPersist);
	}

	@Override
	public boolean canCompute(Technology technology) {
		return usesGitHubRepository(technology) && githubClient.canComputeIndicator();
	}

}
