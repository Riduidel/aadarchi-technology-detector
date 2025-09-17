package com.zenika.tech.lab.ingester.indicators.github.issues;

import com.zenika.tech.lab.ingester.indicators.github.AbstractGitHubIndicatorComputer;
import com.zenika.tech.lab.ingester.indicators.github.graphql.GitHubGraphqlFacade;
import com.zenika.tech.lab.ingester.indicators.github.graphql.entities.Author;
import com.zenika.tech.lab.ingester.indicators.github.graphql.entities.issues.IssueNode;
import com.zenika.tech.lab.ingester.indicators.github.graphql.entities.issues.RepositoryWithIssueCountHistory;
import com.zenika.tech.lab.ingester.model.IndicatorNamed;
import com.zenika.tech.lab.ingester.model.IndicatorRepositoryFacade;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.util.Pair;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import java.util.function.Predicate;

@ApplicationScoped
public class GitHubIssuesIndicatorComputer extends AbstractGitHubIndicatorComputer<Issue, RepositoryWithIssueCountHistory> {

    public static final String GITHUB_ISSUES = "github.issues";

    public GitHubIssuesIndicatorComputer() {
        super(null, null, null, GITHUB_ISSUES);
    }

    @Inject
    public GitHubIssuesIndicatorComputer(@IndicatorNamed(GITHUB_ISSUES) IndicatorRepositoryFacade indicators,
                                         IssueRepository repository,
                                         GitHubGraphqlFacade githubClient) {
        super(indicators, repository, githubClient, GITHUB_ISSUES);
    }

    @Override
    protected Issue toEntity(Pair<String> ownerAndRepositoryName, Object rawEvent) {
        if(!(rawEvent instanceof IssueNode(OffsetDateTime createdAt, Author author))) {
            throw new IllegalArgumentException("Expected IssueEvent, got "+rawEvent.getClass().getName());
        }
        return new Issue(
                ownerAndRepositoryName.getLeft(), ownerAndRepositoryName.getRight(),
                Date.from(createdAt.toInstant()),
                author.login()
        );
    }

    @Override
    protected List<?> getEvents(RepositoryWithIssueCountHistory repositoryPage) {
        return repositoryPage.issues().nodes();
    }

    @Override
    protected int getTotalCountAsOfTodayFor(Pair<String> ownerAndRepositoryName) {
        return githubClient.getTodayCountAsOfTodayForIssues(ownerAndRepositoryName.getLeft(), ownerAndRepositoryName.getRight());
    }

    @Override
    protected void getHistoryCountFor(Pair<String> ownerAndRepositoryName, boolean forceRedownload, Predicate<RepositoryWithIssueCountHistory> processIndicator) {
        githubClient.getHistoryCountForIssues(ownerAndRepositoryName.getLeft(), ownerAndRepositoryName.getRight(), forceRedownload, processIndicator);
    }
}

