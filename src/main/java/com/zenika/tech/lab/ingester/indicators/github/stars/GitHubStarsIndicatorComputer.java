package com.zenika.tech.lab.ingester.indicators.github.stars;

import com.zenika.tech.lab.ingester.indicators.AbstractGitHubIndicatorComputer;
import com.zenika.tech.lab.ingester.indicators.github.graphql.GitHubGraphqlFacade;
import com.zenika.tech.lab.ingester.indicators.github.graphql.User;
import com.zenika.tech.lab.ingester.indicators.github.graphql.entities.stargazer.RepositoryWithStargazerCountHistory;
import com.zenika.tech.lab.ingester.indicators.github.graphql.entities.stargazer.StargazerEvent;
import com.zenika.tech.lab.ingester.model.IndicatorNamed;
import com.zenika.tech.lab.ingester.model.IndicatorRepositoryFacade;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.util.Pair;

import java.util.Date;
import java.util.List;
import java.util.function.Predicate;

@ApplicationScoped
public class GitHubStarsIndicatorComputer extends AbstractGitHubIndicatorComputer<Stargazer, RepositoryWithStargazerCountHistory> {

    public static final String GITHUB_STARS = "github.stars";

    public GitHubStarsIndicatorComputer() {
        super(null, null, null, GITHUB_STARS);
    }

    @Inject
    public GitHubStarsIndicatorComputer(@IndicatorNamed(GITHUB_STARS) IndicatorRepositoryFacade indicators,
                                        StargazerRepository repository,
                                        GitHubGraphqlFacade githubClient) {
        super(indicators, repository, githubClient, GITHUB_STARS);
    }

    @Override
    protected Stargazer toEntity(Pair<String> ownerAndRepositoryName, Object rawEvent) {
        if (!(rawEvent instanceof StargazerEvent(Date starredAt, User node))) {
            throw new IllegalArgumentException("Expected StargazerEvent, got " + rawEvent.getClass().getName());
        }
        return new Stargazer(
                ownerAndRepositoryName.getLeft(), ownerAndRepositoryName.getRight(),
                starredAt,
                node.login
        );
    }

    @Override
    protected List<?> getEvents(RepositoryWithStargazerCountHistory repositoryPage) {
        return repositoryPage.stargazers().edges();
    }

    @Override
    protected int getTotalCountAsOfTodayFor(Pair<String> ownerAndRepositoryName) {
        return githubClient.getTodayCountAsOfTodayForStargazers(ownerAndRepositoryName.getLeft(), ownerAndRepositoryName.getRight());
    }

    @Override
    protected void getHistoryCountFor(Pair<String> ownerAndRepositoryName, boolean forceRedownload, Predicate<RepositoryWithStargazerCountHistory> processIndicator) {
        githubClient.getHistoryCountForStargazers(ownerAndRepositoryName.getLeft(), ownerAndRepositoryName.getRight(), forceRedownload, processIndicator);
    }
}
