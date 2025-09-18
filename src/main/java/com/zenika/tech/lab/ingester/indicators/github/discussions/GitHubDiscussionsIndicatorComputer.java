package com.zenika.tech.lab.ingester.indicators.github.discussions;

import com.zenika.tech.lab.ingester.indicators.github.AbstractGitHubIndicatorComputer;
import com.zenika.tech.lab.ingester.indicators.github.graphql.GitHubGraphqlFacade;
import com.zenika.tech.lab.ingester.indicators.github.graphql.entities.Author;
import com.zenika.tech.lab.ingester.indicators.github.graphql.entities.discussions.DiscussionNode;
import com.zenika.tech.lab.ingester.indicators.github.graphql.entities.discussions.RepositoryWithDiscussionCountHistory;
import com.zenika.tech.lab.ingester.model.IndicatorNamed;
import com.zenika.tech.lab.ingester.model.IndicatorRepositoryFacade;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import org.apache.camel.util.Pair;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import java.util.function.Predicate;

@Dependent
public class GitHubDiscussionsIndicatorComputer extends AbstractGitHubIndicatorComputer<Discussion, RepositoryWithDiscussionCountHistory> {

	public static final String GITHUB_DISCUSSIONS = "github.discussions";

	@Inject
	public GitHubDiscussionsIndicatorComputer(@IndicatorNamed(GITHUB_DISCUSSIONS) IndicatorRepositoryFacade indicators,
											  DiscussionRepository repository,
											  GitHubGraphqlFacade githubClient) {
		super(indicators, repository, githubClient, GITHUB_DISCUSSIONS);
	}

	@Override
	protected Discussion toEntity(Pair<String> ownerAndRepositoryName, Object rawEvent) {
		if (!(rawEvent instanceof DiscussionNode(OffsetDateTime createdAt, Author author))) {
			throw new IllegalArgumentException("Expected DiscussionNode, got " + rawEvent.getClass().getName());
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

	@Override
	protected int getTotalCountAsOfTodayFor(Pair<String> ownerAndRepositoryName) {
		return githubClient.getTodayCountAsOfTodayForDiscussions(ownerAndRepositoryName.getLeft(), ownerAndRepositoryName.getRight());
	}

	@Override
	protected void getHistoryCountFor(Pair<String> ownerAndRepositoryName, boolean forceRedownload, Predicate<RepositoryWithDiscussionCountHistory> processIndicator) {
		githubClient.getHistoryCountForDiscussions(ownerAndRepositoryName.getLeft(), ownerAndRepositoryName.getRight(), forceRedownload, processIndicator);
	}
}

