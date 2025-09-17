package com.zenika.tech.lab.ingester.indicators;

import com.zenika.tech.lab.ingester.indicators.github.AbstractGitHubEndpointRouteBuilder;
import com.zenika.tech.lab.ingester.indicators.github.GitHubBased;
import com.zenika.tech.lab.ingester.indicators.github.GithubIndicatorRepository;
import com.zenika.tech.lab.ingester.indicators.github.HasGithubIndicatorId;
import com.zenika.tech.lab.ingester.indicators.github.graphql.GitHubGraphqlException;
import com.zenika.tech.lab.ingester.indicators.github.graphql.GitHubGraphqlFacade;
import com.zenika.tech.lab.ingester.indicators.github.graphql.entities.PageableHistory;
import com.zenika.tech.lab.ingester.indicators.github.graphql.entities.TodayCount;
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

public abstract class AbstractGitHubIndicatorComputer<T extends HasGithubIndicatorId, U extends TodayCount, V extends PageableHistory> extends AbstractGitHubEndpointRouteBuilder implements IndicatorComputer, GitHubBased {

    public final String githubIndicatorsName;
	private final String routeName;


    @ConfigProperty(name = "tech-lab-ingester.github.stars.missing-count-percentage-threshold", defaultValue = "10")
    int missingCountPercentageThreshold;
	private final IndicatorRepositoryFacade indicators;
	private final GitHubGraphqlFacade githubClient;
	private final GithubIndicatorRepository<T> repository;
    private final Class<U> todayCountClass;
    private final Class<V> historyCountClass;
    private final String todayGraphqlQuery;
    private final String historyGraphqlQuery;

    protected AbstractGitHubIndicatorComputer(
            IndicatorRepositoryFacade indicators,
            GithubIndicatorRepository<T> repository,
            Class<U> todayCountClass,
            Class<V> historyCountClass,
            GitHubGraphqlFacade githubClient,
            String todayGraphqlQuery, String historyGraphqlQuery,
            String githubIndicatorsName) {

        this.indicators = indicators;
        this.repository = repository;
        this.todayCountClass = todayCountClass;
        this.historyCountClass = historyCountClass;
        this.githubClient = githubClient;
        this.todayGraphqlQuery = todayGraphqlQuery;
        this.historyGraphqlQuery = historyGraphqlQuery;
        this.githubIndicatorsName = githubIndicatorsName;

        this.routeName = "compute-"+ githubIndicatorsName.replace('.', '-');
    }

    @Override
	public void configure() throws Exception {
		super.configureExceptions();
		from(getFromRoute())
			.routeId(routeName)
			.idempotentConsumer()
				.body(Technology.class, t -> String.format("%s-%s", githubIndicatorsName, t.repositoryUrl))
				.idempotentRepository(MemoryIdempotentRepository.memoryIdempotentRepository(10*2))
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
		} catch(GitHubGraphqlException e) {
			Log.warnf(e, "Unable to fetch stars for missing repo");
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
		int remoteCount = githubClient.getTodayCountFor(ownerAndRepositoryName.getLeft(), ownerAndRepositoryName.getRight(), todayGraphqlQuery, todayCountClass);
		int missingCountPercentage = (int) (((remoteCount-localCount)/(remoteCount*1.0))*100.0);
		boolean forceRedownload = missingCountPercentage > missingCountPercentageThreshold;
		if(forceRedownload) {
			Log.infof("📥 For %s/%s, we have %d stars locally, and there are %d stars on GitHub (we lack %d %%). Forcing full redownload", ownerAndRepositoryName.getLeft(), ownerAndRepositoryName.getRight(), localCount, remoteCount, missingCountPercentage);
		} else {
			Log.infof("For %s/%s, we have %d stars locally, and there are %d stars on GitHub (we lack %d %%)", ownerAndRepositoryName.getLeft(), ownerAndRepositoryName.getRight(), localCount, remoteCount, missingCountPercentage);
		}
		boolean shouldDownloadStars = localCount<remoteCount;
		if(shouldDownloadStars) {
			AtomicInteger processedCount = new AtomicInteger();
			githubClient.getHistoryCountFor(ownerAndRepositoryName.getLeft(), ownerAndRepositoryName.getRight(), historyGraphqlQuery, forceRedownload, historyCountClass,
				repositoryPage -> {
					try {
						processedCount.addAndGet(getEvents(repositoryPage).size());
						return this.processPage(ownerAndRepositoryName, repositoryPage);
					} finally {
						if(Log.isDebugEnabled()) {
							Log.debugf("Processed %d elements. Written %d/%d stargazers of %s/%s",
									processedCount.intValue(),
									repository.count(ownerAndRepositoryName),
									remoteCount,
									ownerAndRepositoryName.getLeft(),
									ownerAndRepositoryName.getRight());
						}
					}
				},  repositoryPage -> repositoryPage == null || repositoryPage.hasNoData());
		}
	}

    private void computeAllPastIndicators(Technology technology, Pair<String> ownerAndRepositoryName) {
        repository.groupIndicatorsByMonths(technology, ownerAndRepositoryName)
                .forEach(indicators::maybePersist);
    }

	/**
	 * Process one page of graphql query result
	 * @param ownerAndRepositoryName pair of owner and repository name
	 * @param repositoryPage page to process
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
	 * @param ownerAndRepositoryName pair of owner and repository name
	 * @param event event to persist
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

    protected abstract T toEntity(Pair<String> ownerAndRepositoryName, Object rawEvent);

    protected abstract List<? extends Object> getEvents(V repositoryPage);

}
