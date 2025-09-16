package org.ndx.aadarchi.technology.detector.indicators.github.discussions;

import com.zenika.tech.lab.ingester.indicators.IndicatorComputer;
import com.zenika.tech.lab.ingester.indicators.github.AbstractGitHubEndpointRouteBuilder;
import com.zenika.tech.lab.ingester.indicators.github.GitHubBased;
import com.zenika.tech.lab.ingester.indicators.github.graphql.GitHubGraphqlFacade;
import com.zenika.tech.lab.ingester.indicators.github.graphql.entities.RepositoryWithDiscussionList;
import com.zenika.tech.lab.ingester.model.IndicatorNamed;
import com.zenika.tech.lab.ingester.model.IndicatorRepositoryFacade;
import com.zenika.tech.lab.ingester.model.Technology;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.Exchange;
import org.apache.camel.builder.endpoint.dsl.DirectEndpointBuilderFactory;
import org.apache.camel.support.processor.idempotent.MemoryIdempotentRepository;
import org.apache.camel.util.Pair;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

@ApplicationScoped
public class GitHubDiscussionsIndicatorComputer extends AbstractGitHubEndpointRouteBuilder implements IndicatorComputer, GitHubBased {

    public static final String GITHUB_DISCUSSIONS = "github.discussions";
    private static final String ROUTE_NAME = "compute-"+ GITHUB_DISCUSSIONS.replace('.', '-');

    @Inject @IndicatorNamed(GITHUB_DISCUSSIONS)
    IndicatorRepositoryFacade indicators;
    @Inject
    DiscussionRepository discussionRepository;
    @Inject
    GitHubGraphqlFacade githubClient;

    @Override
    public void configure() throws Exception {
    	super.configureExceptions();
        from(getFromRoute())
            .routeId(ROUTE_NAME)
			.idempotentConsumer()
				.body(Technology.class, t -> String.format("%s-%s", GITHUB_DISCUSSIONS, t.repositoryUrl))
				.idempotentRepository(MemoryIdempotentRepository.memoryIdempotentRepository(10*2))
            .process(this::computeGitHubDiscussions)
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

    private void computeGitHubDiscussions(Exchange exchange) throws IOException {
        computeGitHubDiscussions(exchange.getMessage().getBody(Technology.class));
    }

    private void computeGitHubDiscussions(Technology technology) throws IOException {
        computePastDiscussions(technology);
    }

    private void computePastDiscussions(Technology technology) throws IOException {
        getRepository(technology).ifPresent(pair -> {
            loadAllPastDiscussions(pair);
            computeAllPastDiscussions(technology, pair);
        });
    }

    private void computeAllPastDiscussions(Technology technology, Pair<String> pair) {
        discussionRepository.groupDiscussionsByMonths(technology, pair)
                .forEach(indicator -> indicators.maybePersist(indicator));
    }

    private void loadAllPastDiscussions(Pair<String> path) {
        long localCount = discussionRepository.count(path);
        int remoteCount = githubClient.getCurrentTotalNumberOfDiscussion(path.getLeft(), path.getRight());
        int missingCountPercentage = (remoteCount > 0) ? (int) (((remoteCount - localCount) / (remoteCount * 1.0)) * 100.0) : 0;
        boolean forceRedownload = missingCountPercentage > 10;
        if(forceRedownload) {
            Log.infof("📥 For %s/%s, we have %d discussions locally, and there are %d discussions on GitHub (we lack %d %%). Forcing full redownload", path.getLeft(), path.getRight(), localCount, remoteCount, missingCountPercentage);
        } else {
            Log.infof("For %s/%s, we have %d discussions locally, and there are %d discussions on GitHub (we lack %d %%)", path.getLeft(), path.getRight(), localCount, remoteCount, missingCountPercentage);
        }
		boolean shouldDownloadStars = localCount<remoteCount;
		if(shouldDownloadStars) {
            AtomicInteger processedCount = new AtomicInteger();
            githubClient.getAllDiscussions(path.getLeft(), path.getRight(), forceRedownload,
                    discussionListPage -> {
                        try {
    						processedCount.addAndGet(discussionListPage.discussions.nodes.size());
                            return this.processPage(path, discussionListPage);
                        } finally {
    						if(Log.isDebugEnabled()) {
    							Log.debugf("Processed %d elements. Written %d/%d discussions of %s/%s",
                                    processedCount.intValue(),
                                    discussionRepository.count(path),
                                    remoteCount,
                                    path.getLeft(),
                                    path.getRight());
    						}
                        }
                    });
        }
    }


    /**
     * Process one page of graphql query result for discussions.
     * @param path Repository path (owner/name)
     * @param discussionsListPage The page data received from GraphQL
     * @return true if we have to continue the process (if at least one discussions event was persisted)
     */
    private boolean processPage(Pair<String> path, RepositoryWithDiscussionList discussionsListPage) {
        if (discussionsListPage.discussions == null || discussionsListPage.discussions.nodes == null) {
            Log.warnf("Received an empty or invalid discussion page for %s/%s", path.getLeft(), path.getRight());
            return false;
        }
        return discussionsListPage.discussions.nodes
                .stream()
                .map(discussionNode -> maybePersistDiscussion(path, discussionNode))
                .reduce(false, (a, b) -> a || b); // Default to false if stream is empty
    }

    /**
     * Persist discussion event if it doesn't exist.
     * @param path Repository path (owner/name)
     * @param discussionNode The discussion node data from GraphQL
     * @return true if database changed, false if event already existed in db
     */
    private boolean maybePersistDiscussion(Pair<String> path, RepositoryWithDiscussionList.DiscussionNode discussionNode) {
        Discussion toPersist = new Discussion(
                path.getLeft(), path.getRight(),
                Date.from(discussionNode.createdAt.toInstant()),
                discussionNode.author.login
        );
        return discussionRepository.maybePersist(toPersist);
    }

	@Override
	public boolean canCompute(Technology technology) {
		return usesGitHubRepository(technology) && githubClient.canComputeIndicator();
	}
}

