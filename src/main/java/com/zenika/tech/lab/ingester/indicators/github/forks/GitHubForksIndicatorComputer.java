package com.zenika.tech.lab.ingester.indicators.github.forks;

import com.zenika.tech.lab.ingester.Configuration;
import com.zenika.tech.lab.ingester.indicators.AbstractGitHubIndicatorComputer;
import com.zenika.tech.lab.ingester.indicators.github.graphql.GitHubGraphqlFacade;
import com.zenika.tech.lab.ingester.indicators.github.graphql.entities.Owner;
import com.zenika.tech.lab.ingester.indicators.github.graphql.entities.forks.ForkNode;
import com.zenika.tech.lab.ingester.indicators.github.graphql.entities.forks.RepositoryWithForkCountHistory;
import com.zenika.tech.lab.ingester.indicators.github.graphql.entities.forks.RepositoryWithForkCountToday;
import com.zenika.tech.lab.ingester.model.IndicatorNamed;
import com.zenika.tech.lab.ingester.model.IndicatorRepositoryFacade;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.util.Pair;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;

@ApplicationScoped
public class GitHubForksIndicatorComputer extends AbstractGitHubIndicatorComputer<Fork, RepositoryWithForkCountToday, RepositoryWithForkCountHistory> {

    public static final String GITHUB_FORKS = "github.forks";

    public GitHubForksIndicatorComputer() {
        super(null, null, null, null, null, null, null, GITHUB_FORKS);
    }

    @Inject
    public GitHubForksIndicatorComputer(
            @IndicatorNamed(GITHUB_FORKS) IndicatorRepositoryFacade indicators,
            ForkRepository repository,
            GitHubGraphqlFacade githubClient,
            @ConfigProperty(name = Configuration.INDICATORS_PREFIX + "github.forks.graphql.today") String todayGraphqlQuery,
            @ConfigProperty(name = Configuration.INDICATORS_PREFIX + "github.forks.graphql.history") String historyGraphqlQuery) {
        super(indicators, repository, RepositoryWithForkCountToday.class, RepositoryWithForkCountHistory.class, githubClient, todayGraphqlQuery, historyGraphqlQuery, GITHUB_FORKS);
    }

    @Override
    protected Fork toEntity(Pair<String> ownerAndRepositoryName, Object rawEvent) {

        if(!(rawEvent instanceof ForkNode(OffsetDateTime createdAt, Owner owner))){
            throw new IllegalArgumentException("Expected ForkNode, got "+rawEvent.getClass().getName());
        }
        return new Fork(
                ownerAndRepositoryName.getLeft(), ownerAndRepositoryName.getRight(),
                Date.from(createdAt.toInstant()),
                owner.login());
    }

    @Override
    protected List<?> getEvents(RepositoryWithForkCountHistory repositoryPage) {
        return repositoryPage.forks().nodes();
    }
}

