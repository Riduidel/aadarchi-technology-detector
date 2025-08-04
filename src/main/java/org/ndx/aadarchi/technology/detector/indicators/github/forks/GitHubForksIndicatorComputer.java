package org.ndx.aadarchi.technology.detector.indicators.github.forks;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.camel.Exchange;
import org.apache.camel.builder.endpoint.dsl.DirectEndpointBuilderFactory;
import org.apache.camel.support.processor.idempotent.MemoryIdempotentRepository;
import org.apache.camel.util.Pair;
import org.ndx.aadarchi.technology.detector.indicators.IndicatorComputer;
import org.ndx.aadarchi.technology.detector.indicators.github.AbstractGitHubEndpointRouteBuilder;
import org.ndx.aadarchi.technology.detector.indicators.github.GitHubBased;
import org.ndx.aadarchi.technology.detector.indicators.github.graphql.GitHubGraphqlException;
import org.ndx.aadarchi.technology.detector.indicators.github.graphql.GitHubGraphqlFacade;
import org.ndx.aadarchi.technology.detector.indicators.github.graphql.entities.RepositoryWithForkList;
import org.ndx.aadarchi.technology.detector.model.IndicatorNamed;
import org.ndx.aadarchi.technology.detector.model.IndicatorRepositoryFacade;
import org.ndx.aadarchi.technology.detector.model.Technology;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class GitHubForksIndicatorComputer extends AbstractGitHubEndpointRouteBuilder  implements IndicatorComputer, GitHubBased {

    public static final String GITHUB_FORKS = "github.forks";
    private static final String ROUTE_NAME = "compute-"+GITHUB_FORKS.replace('.', '-');

    @Inject @IndicatorNamed(GITHUB_FORKS) IndicatorRepositoryFacade indicators;
    @Inject ForkRepository forksRepository;
    @Inject GitHubGraphqlFacade githubClient;

    @Override
    public void configure() throws Exception {
    	super.configureExceptions();
        from(getFromRoute())
            .routeId(ROUTE_NAME)
			.idempotentConsumer()
				.body(Technology.class, t -> String.format("%s-%s", GITHUB_FORKS, t.repositoryUrl))
				.idempotentRepository(MemoryIdempotentRepository.memoryIdempotentRepository(10*2))
            .process(this::computeGitHubForks)
			.end()
        ;
    }

    private DirectEndpointBuilderFactory.DirectEndpointBuilder getFromRoute() {
        return direct(ROUTE_NAME);
    }

    @Override
    public String getFromRouteName() {
        return getFromRoute().getUri();
    }

    private void computeGitHubForks(Exchange exchange) throws IOException {
		try {
			computeGitHubForks(exchange.getMessage().getBody(Technology.class));
		} catch(GitHubGraphqlException e) {
//			Log.warnf(e, "Unable to fetch stars for missing repo");
			throw e;
		}
    }

    private void computeGitHubForks(Technology technology) throws IOException {
        computePastForks(technology);
    }

    private void computePastForks(Technology technology) throws IOException {
        getRepository(technology).ifPresent(pair -> {
            loadAllPastForks(pair);
            computeAllPastForks(technology, pair);
        });
    }

    private void computeAllPastForks(Technology technology, Pair<String> pair) {
        forksRepository.groupForksByMonths(technology, pair).stream()
                .forEach(indicator -> indicators.maybePersist(indicator));
    }

    private void loadAllPastForks(Pair<String> path) {
        long localCount = forksRepository.count(path);
        int remoteCount = githubClient.getCurrentTotalNumberOfFork(path.getLeft(), path.getRight());
        int missingCountPercentage = (remoteCount > 0) ? (int) (((remoteCount - localCount) / (remoteCount * 1.0)) * 100.0) : 0;
        boolean forceRedownload = missingCountPercentage > 10;
        if(forceRedownload) {
            Log.infof("📥 For %s/%s, we have %d forks locally, and there are %d forks on GitHub (we lack %d %%). Forcing full redownload", path.getLeft(), path.getRight(), localCount, remoteCount, missingCountPercentage);
        } else {
            Log.infof("For %s/%s, we have %d forks locally, and there are %d forks on GitHub (we lack %d %%)", path.getLeft(), path.getRight(), localCount, remoteCount, missingCountPercentage);
        }
		boolean shouldDownloadStars = localCount<remoteCount;
		if(shouldDownloadStars) {
            AtomicInteger processedCount = new AtomicInteger();
            githubClient.getAllForks(path.getLeft(), path.getRight(), forceRedownload,
                    forkListPage -> {
                        try {
    						processedCount.addAndGet(forkListPage.forks.nodes.size());
                            return this.processPage(path, forkListPage);
                        } finally {
    						if(Log.isDebugEnabled()) {
    							Log.debugf("Processed %d elements. Written %d/%d stargazers of %s/%s", 
                                    processedCount.intValue(),
                                    forksRepository.count(path),
                                    remoteCount,
                                    path.getLeft(),
                                    path.getRight());
    						}
                        }
                    });
        }
    }


    /**
     * Process one page of graphql query result for forks.
     * @param path Repository path (owner/name)
     * @param forkListPage The page data received from GraphQL
     * @return true if we have to continue the process (if at least one fork event was persisted)
     */
    private boolean processPage(Pair<String> path, RepositoryWithForkList forkListPage) {
        if (forkListPage.forks == null || forkListPage.forks.nodes == null) {
            Log.warnf("Received an empty or invalid fork page for %s/%s", path.getLeft(), path.getRight());
            return false;
        }
        return forkListPage.forks.nodes
                .stream()
                .map(forkNode -> maybePersistFork(path, forkNode))
                .collect(Collectors.reducing(false, (a, b) -> a || b)); // Default to false if stream is empty
    }

    /**
     * Persist fork event if it doesn't exist.
     * @param path Repository path (owner/name)
     * @param forkNode The fork node data from GraphQL
     * @return true if database changed, false if event already existed in db
     */
    private boolean maybePersistFork(Pair<String> path, RepositoryWithForkList.ForkNode forkNode) {
        Fork toPersist = new Fork(
                path.getLeft(), path.getRight(),
                Date.from(forkNode.createdAt.toInstant()),
                forkNode.owner.login
        );
        return forksRepository.maybePersist(toPersist);
    }

	@Override
	public boolean canCompute(Technology technology) {
		return usesGitHubRepository(technology) && githubClient.canComputeIndicator();
	}
}

