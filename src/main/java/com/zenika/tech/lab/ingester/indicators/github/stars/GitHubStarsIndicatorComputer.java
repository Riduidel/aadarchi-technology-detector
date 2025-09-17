package com.zenika.tech.lab.ingester.indicators.github.stars;

import com.zenika.tech.lab.ingester.Configuration;
import com.zenika.tech.lab.ingester.indicators.AbstractGitHubIndicatorComputer;
import com.zenika.tech.lab.ingester.indicators.github.graphql.GitHubGraphqlFacade;
import com.zenika.tech.lab.ingester.indicators.github.graphql.User;
import com.zenika.tech.lab.ingester.indicators.github.graphql.entities.stargazer.RepositoryWithStargazerCountHistory;
import com.zenika.tech.lab.ingester.indicators.github.graphql.entities.stargazer.RepositoryWithStargazerCountToday;
import com.zenika.tech.lab.ingester.indicators.github.graphql.entities.stargazer.StargazerEvent;
import com.zenika.tech.lab.ingester.model.IndicatorNamed;
import com.zenika.tech.lab.ingester.model.IndicatorRepositoryFacade;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.util.Pair;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Date;
import java.util.List;

@ApplicationScoped
public class GitHubStarsIndicatorComputer extends AbstractGitHubIndicatorComputer<Stargazer, RepositoryWithStargazerCountToday, RepositoryWithStargazerCountHistory> {

    public static final String GITHUB_STARS = "github.stars";

    public GitHubStarsIndicatorComputer() {
        super(null, null, null, null, null, null, null, GITHUB_STARS);
    }

    @Inject
    public GitHubStarsIndicatorComputer(@IndicatorNamed(GITHUB_STARS) IndicatorRepositoryFacade indicators,
                                        StargazerRepository repository,
                                        GitHubGraphqlFacade githubClient,
                                        @ConfigProperty(name = Configuration.INDICATORS_PREFIX + "github.stars.graphql.today") String githubStarsToday,
                                        @ConfigProperty(name = Configuration.INDICATORS_PREFIX + "github.stars.graphql.history") String githubStarsHistory) {
        super(indicators, repository, RepositoryWithStargazerCountToday.class, RepositoryWithStargazerCountHistory.class, githubClient, githubStarsToday, githubStarsHistory, GITHUB_STARS);
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

}
