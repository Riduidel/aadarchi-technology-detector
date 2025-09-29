package com.zenika.tech.lab.ingester.indicators.github.forks;

import com.zenika.tech.lab.ingester.indicators.github.AbstractGitHubIndicatorComputer;
import com.zenika.tech.lab.ingester.indicators.github.graphql.GitHubGraphqlFacade;
import com.zenika.tech.lab.ingester.indicators.github.graphql.entities.Owner;
import com.zenika.tech.lab.ingester.indicators.github.graphql.entities.forks.ForkNode;
import com.zenika.tech.lab.ingester.indicators.github.graphql.entities.forks.RepositoryWithForkCountHistory;
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
public class GitHubForksIndicatorComputer extends AbstractGitHubIndicatorComputer<Fork, RepositoryWithForkCountHistory> {

	public static final String GITHUB_FORKS = "github.forks";

	@Inject
	public GitHubForksIndicatorComputer(
			@IndicatorNamed(GITHUB_FORKS) IndicatorRepositoryFacade indicators,
			ForkRepository repository,
			GitHubGraphqlFacade githubClient) {
		super(indicators, repository, githubClient, GITHUB_FORKS);
	}

	@Override
	protected Fork toEntity(Pair<String> ownerAndRepositoryName, Object rawEvent) {

		if (!(rawEvent instanceof ForkNode(OffsetDateTime createdAt, Owner owner))) {
			throw new IllegalArgumentException("Expected ForkNode, got " + rawEvent.getClass().getName());
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

	@Override
	protected int getTotalCountAsOfTodayFor(Pair<String> ownerAndRepositoryName) {
		return githubClient.getTodayCountAsOfTodayForForks(ownerAndRepositoryName.getLeft(), ownerAndRepositoryName.getRight());
	}

	@Override
	protected void getHistoryCountFor(Pair<String> ownerAndRepositoryName, boolean forceRedownload, Predicate<RepositoryWithForkCountHistory> processIndicator) {
		githubClient.getHistoryCountForForks(ownerAndRepositoryName.getLeft(), ownerAndRepositoryName.getRight(), forceRedownload, processIndicator);
	}
}

