package com.zenika.tech.lab.ingester.indicators.github.discussions;

import com.zenika.tech.lab.ingester.Configuration;
import com.zenika.tech.lab.ingester.indicators.AbstractGitHubIndicatorComputer;
import com.zenika.tech.lab.ingester.indicators.github.graphql.GitHubGraphqlFacade;
import com.zenika.tech.lab.ingester.indicators.github.graphql.entities.Author;
import com.zenika.tech.lab.ingester.indicators.github.graphql.entities.discussions.DiscussionNode;
import com.zenika.tech.lab.ingester.indicators.github.graphql.entities.discussions.RepositoryWithDiscussionCountHistory;
import com.zenika.tech.lab.ingester.indicators.github.graphql.entities.discussions.RepositoryWithDiscussionCountToday;
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
public class GitHubDiscussionsIndicatorComputer extends AbstractGitHubIndicatorComputer<Discussion, RepositoryWithDiscussionCountToday, RepositoryWithDiscussionCountHistory> {

    public static final String GITHUB_DISCUSSIONS = "github.discussions";

    public GitHubDiscussionsIndicatorComputer() {
        super(null, null, null, null, null, null, null, GITHUB_DISCUSSIONS);
    }

    @Inject
    public GitHubDiscussionsIndicatorComputer(@IndicatorNamed(GITHUB_DISCUSSIONS) IndicatorRepositoryFacade indicators,
                                              DiscussionRepository repository,
                                              GitHubGraphqlFacade githubClient,
                                              @ConfigProperty(name = Configuration.INDICATORS_PREFIX+"github.discussions.graphql.today") String todayGraphqlQuery,
                                              @ConfigProperty(name = Configuration.INDICATORS_PREFIX+"github.discussions.graphql.history") String historyGraphqlQuery) {
        super(indicators, repository, RepositoryWithDiscussionCountToday.class, RepositoryWithDiscussionCountHistory.class, githubClient, todayGraphqlQuery, historyGraphqlQuery, GITHUB_DISCUSSIONS);
    }

    @Override
    protected Discussion toEntity(Pair<String> ownerAndRepositoryName, Object rawEvent) {
        if(!(rawEvent instanceof DiscussionNode(OffsetDateTime createdAt, Author author))) {
            throw new IllegalArgumentException("Expected DiscussionNode, got "+rawEvent.getClass().getName());
        }
        return new Discussion(
                ownerAndRepositoryName.getLeft(), ownerAndRepositoryName.getRight(),
                Date.from(createdAt.toInstant()),
                author.login()
        );
    }

    @Override
    protected List<?> getEvents(RepositoryWithDiscussionCountHistory repositoryPage) {
        return repositoryPage.discussions().nodes();
    }
}

