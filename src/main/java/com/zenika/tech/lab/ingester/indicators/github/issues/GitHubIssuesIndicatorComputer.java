package com.zenika.tech.lab.ingester.indicators.github.issues;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.camel.Exchange;
import org.apache.camel.builder.endpoint.dsl.DirectEndpointBuilderFactory;
import org.apache.camel.support.processor.idempotent.MemoryIdempotentRepository;
import org.apache.camel.util.Pair;

import com.zenika.tech.lab.ingester.indicators.IndicatorComputer;
import com.zenika.tech.lab.ingester.indicators.github.AbstractGitHubEndpointRouteBuilder;
import com.zenika.tech.lab.ingester.indicators.github.GitHubBased;
import com.zenika.tech.lab.ingester.indicators.github.graphql.GitHubGraphqlFacade;
import com.zenika.tech.lab.ingester.indicators.github.graphql.entities.RepositoryWithIssueList;
import com.zenika.tech.lab.ingester.model.IndicatorNamed;
import com.zenika.tech.lab.ingester.model.IndicatorRepositoryFacade;
import com.zenika.tech.lab.ingester.model.Technology;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class GitHubIssuesIndicatorComputer extends AbstractGitHubEndpointRouteBuilder  implements IndicatorComputer, GitHubBased {

    public static final String GITHUB_ISSUES = "github.issues";
    private static final String ROUTE_NAME = "compute-"+ GITHUB_ISSUES.replace('.', '-');

    @Inject @IndicatorNamed(GITHUB_ISSUES) IndicatorRepositoryFacade indicators;
    @Inject
    IssueRepository issueRepository;
    @Inject GitHubGraphqlFacade githubClient;

    @Override
    public void configure() throws Exception {
    	super.configureExceptions();
        from(getFromRoute())
            .routeId(ROUTE_NAME)
			.idempotentConsumer()
				.body(Technology.class, t -> String.format("%s-%s", GITHUB_ISSUES, t.repositoryUrl))
				.idempotentRepository(MemoryIdempotentRepository.memoryIdempotentRepository(10*2))
            .process(this::computeGitHubIssues)
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

    private void computeGitHubIssues(Exchange exchange) throws IOException {
        computeGitHubIssues(exchange.getMessage().getBody(Technology.class));
    }

    private void computeGitHubIssues(Technology technology) throws IOException {
        computePastIssues(technology);
    }

    private void computePastIssues(Technology technology) throws IOException {
        getRepository(technology).ifPresent(pair -> {
            loadAllPastIssues(pair);
            computeAllPastIssues(technology, pair);
        });
    }

    private void computeAllPastIssues(Technology technology, Pair<String> pair) {
        issueRepository.groupIssuesByMonths(technology, pair)
                .forEach(indicator -> indicators.maybePersist(indicator));
    }

    private void loadAllPastIssues(Pair<String> path) {
        long localCount = issueRepository.count(path);
        int remoteCount = githubClient.getCurrentTotalNumberOfIssue(path.getLeft(), path.getRight());
        int missingCountPercentage = (remoteCount > 0) ? (int) (((remoteCount - localCount) / (remoteCount * 1.0)) * 100.0) : 0;
        boolean forceRedownload = missingCountPercentage > 10;
        if(forceRedownload) {
            Log.infof("📥 For %s/%s, we have %d issues locally, and there are %d issues on GitHub (we lack %d %%). Forcing full redownload", path.getLeft(), path.getRight(), localCount, remoteCount, missingCountPercentage);
        } else {
            Log.infof("For %s/%s, we have %d issues locally, and there are %d issues on GitHub (we lack %d %%)", path.getLeft(), path.getRight(), localCount, remoteCount, missingCountPercentage);
        }
		boolean shouldDownloadStars = localCount<remoteCount;
		if(shouldDownloadStars) {
            AtomicInteger processedCount = new AtomicInteger();
            githubClient.getAllIssues(path.getLeft(), path.getRight(), forceRedownload,
                    issueListPage -> {
                        try {
    						processedCount.addAndGet(issueListPage.issues.nodes.size());
                            return this.processPage(path, issueListPage);
                        } finally {
    						if(Log.isDebugEnabled()) {
    							Log.debugf("Processed %d elements. Written %d/%d issues of %s/%s",
                                    processedCount.intValue(),
                                    issueRepository.count(path),
                                    remoteCount,
                                    path.getLeft(),
                                    path.getRight());
    						}
                        }
                    });
        }
    }


    /**
     * Process one page of graphql query result for issues.
     * @param path Repository path (owner/name)
     * @param issuesListPage The page data received from GraphQL
     * @return true if we have to continue the process (if at least one issues event was persisted)
     */
    private boolean processPage(Pair<String> path, RepositoryWithIssueList issuesListPage) {
        if (issuesListPage.issues == null || issuesListPage.issues.nodes == null) {
            Log.warnf("Received an empty or invalid issue page for %s/%s", path.getLeft(), path.getRight());
            return false;
        }
        return issuesListPage.issues.nodes
                .stream()
                .map(issueNode -> maybePersistIssue(path, issueNode))
                .reduce(false, (a, b) -> a || b); // Default to false if stream is empty
    }

    /**
     * Persist issue event if it doesn't exist.
     * @param path Repository path (owner/name)
     * @param issueNode The issue node data from GraphQL
     * @return true if database changed, false if event already existed in db
     */
    private boolean maybePersistIssue(Pair<String> path, RepositoryWithIssueList.IssueNode issueNode) {
        Issue toPersist = new Issue(
                path.getLeft(), path.getRight(),
                Date.from(issueNode.createdAt.toInstant()),
                issueNode.author.login
        );
        return issueRepository.maybePersist(toPersist);
    }

	@Override
	public boolean canCompute(Technology technology) {
		return usesGitHubRepository(technology) && githubClient.canComputeIndicator();
	}
}

