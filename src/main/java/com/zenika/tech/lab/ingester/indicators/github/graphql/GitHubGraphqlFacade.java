package com.zenika.tech.lab.ingester.indicators.github.graphql;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.zenika.tech.lab.ingester.indicators.github.graphql.entities.PageableHistory;
import com.zenika.tech.lab.ingester.indicators.github.graphql.entities.TodayCount;
import com.zenika.tech.lab.ingester.indicators.github.graphql.entities.forks.RepositoryWithForkCountHistory;
import com.zenika.tech.lab.ingester.indicators.github.graphql.entities.forks.RepositoryWithForkCountToday;
import com.zenika.tech.lab.ingester.indicators.github.graphql.entities.issues.RepositoryWithIssueCountHistory;
import com.zenika.tech.lab.ingester.indicators.github.graphql.entities.issues.RepositoryWithIssueCountToday;
import com.zenika.tech.lab.ingester.indicators.github.graphql.entities.stargazer.RepositoryWithStargazerCountHistory;
import com.zenika.tech.lab.ingester.indicators.github.graphql.entities.stargazer.RepositoryWithStargazerCountToday;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.faulttolerance.Retry;

import com.zenika.tech.lab.ingester.Configuration;
import com.zenika.tech.lab.ingester.indicators.github.graphql.entities.RateLimit;

import io.github.bucket4j.BandwidthBuilder;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.BucketListener;
import io.github.bucket4j.TokensInheritanceStrategy;
import io.quarkus.logging.Log;
import io.smallrye.graphql.client.GraphQLClient;
import io.smallrye.graphql.client.InvalidResponseException;
import io.smallrye.graphql.client.Response;
import io.smallrye.graphql.client.dynamic.api.DynamicGraphQLClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class GitHubGraphqlFacade {
	private static final int TOKEN_LOWER_BOUND = 100;

	@Inject
	@GraphQLClient("github")
	DynamicGraphQLClient dynamicClient;
	
	@ConfigProperty(name = Configuration.INDICATORS_PREFIX+"github.stars.graphql.today")
	String githubStarsToday;
	@ConfigProperty(name = Configuration.INDICATORS_PREFIX+"github.stars.graphql.history")
	String githubStarsHistory;
	@ConfigProperty(name = Configuration.INDICATORS_PREFIX+"github.forks.graphql.today")
	String githubForksToday;
	@ConfigProperty(name = Configuration.INDICATORS_PREFIX+"github.forks.graphql.history")
	String githubForksHistory;
    @ConfigProperty(name = Configuration.INDICATORS_PREFIX+"github.issues.graphql.today")
    String githubIssuesToday;
    @ConfigProperty(name = Configuration.INDICATORS_PREFIX+"github.issues.graphql.history")
    String githubIssuesHistory;

	public class BucketThreadParkedLogger implements BucketListener {

		@Override
		public void onConsumed(long tokens) {}

		@Override
		public void onRejected(long tokens) {}

		@Override
		public void beforeParking(long nanos) {
			LocalDateTime until = LocalDateTime.now().plusNanos(nanos);
			Log.warnf("Thread will be parked by Bucket4J until %s due to bucket having only %d tokens remaining",
					DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM).format(until),
					rateLimitingBucket.getAvailableTokens());
		}

		@Override
		public void onParked(long nanos) {
			Log.warnf("Thread was parked by Bucket4J %s due to bucket having only %d tokens remaining. Operations resume NOW!",
					DurationFormatUtils.formatDuration(TimeUnit.NANOSECONDS.toMillis(nanos),"HH:mm:ss"),
					rateLimitingBucket.getAvailableTokens());
		}

		@Override
		public void onInterrupted(InterruptedException e) {}

		@Override
		public void onDelayed(long nanos) {}

	}

	Bucket rateLimitingBucket = Bucket.builder()
			// We initialize as a classical GitHub user
			.addLimit(limit -> limit
					.capacity(5_000)
					.refillIntervally(5_000, Duration.ofHours(1))
					.id("GitHub")
				)
			.withMillisecondPrecision()
			.build()
			.toListenable(new BucketThreadParkedLogger());



	private GitHubGraphqlException processGraphqlErrors(String graphqlQueryUsed, Map<String, Object> arguments, Response response) {
		List<io.smallrye.graphql.client.GraphQLError> errors = response.getErrors();
		String errorsMessage = errors.stream()
			.map((error -> String.format("\t%s", 
					error.getMessage()))
		).collect(Collectors.joining("\n"));
		String fullMessage = String.format(
				"Request\n"
				+ "%s\n"
				+ "when executed with parameters %s\n"
				+ "generated errors\n------------\n"
				+ "%s\n------------\n",
                graphqlQueryUsed,
				arguments,
				errorsMessage);
		if(errors.size()==1) {
			if(errorsMessage.contains("Could not resolve to a Repository with the name")) {
				return new NoSuchRepository(
						fullMessage, errors);
			} else if(errorsMessage.contains("API rate limit exceeded for user ID")) {
				return new RateLimitExceeded(
						fullMessage, errors,
						new RateLimitMetadata(response));
			}
		}
		return new GitHubGraphqlException(
				fullMessage, errors);
	}


	private Response executeSync(String query, Map<String, Object> arguments, long tokens) throws ExecutionException, InterruptedException {
		rateLimitingBucket.asBlocking().consume(tokens);
		Response returned = dynamicClient.executeSync(query, arguments);
		updateBucketConfiguration(returned, tokens);
		return returned;
	}
	
	public static class RateLimitMetadata {
		public final int tokensPerHour;
		public final int tokensRemaining;
		public final int tokensUsed;
		public final int tokensResetInstant;

		public RateLimitMetadata(Response returned) {
			Map<String, String> metadata = returned.getTransportMeta()
					.entrySet()
					.stream()
					.collect(Collectors.toMap(
							entry -> entry.getKey().toLowerCase(),
							entry -> entry.getValue().stream().collect(Collectors.joining())));
			tokensPerHour = Integer.parseInt(metadata.get("x-ratelimit-limit"));
			tokensRemaining = Integer.parseInt(metadata.get("x-ratelimit-remaining"));
			tokensUsed = Integer.parseInt(metadata.get("x-ratelimit-used"));
			tokensResetInstant = Integer.parseInt(metadata.get("x-ratelimit-reset"));
		}

		public Instant getResetInstant() {
			return Instant.ofEpochSecond(tokensResetInstant);
		}
	}

	private void updateBucketConfiguration(Response returned, long tokens) {
		RateLimitMetadata rateLimitMetadata = new RateLimitMetadata(returned);
		// Immediatly get rate limit cost if possible
		if(returned.hasData() && !returned.hasError()) {
			evaluateRateLimitCost(returned, tokens);
		}
		Instant resetInstant = rateLimitMetadata.getResetInstant();
		if(rateLimitMetadata.tokensRemaining<TOKEN_LOWER_BOUND) {
			Log.warnf("%s tokens remaining. Bucket refulling will happen at %s",
					rateLimitMetadata.tokensRemaining,
					resetInstant.atZone(ZoneId.systemDefault())
					);
		} else {
			Log.debugf("%s tokens remaining locally, and %s tokens remaining on GitHub side.",
					rateLimitingBucket.getAvailableTokens(),
					rateLimitMetadata.tokensRemaining);
		}
		rateLimitingBucket.replaceConfiguration(
				BucketConfiguration.builder()
					.addLimit(BandwidthBuilder
							.builder()
							.capacity(rateLimitMetadata.tokensPerHour)
							.refillIntervallyAligned(rateLimitMetadata.tokensPerHour, Duration.ofHours(1), resetInstant)
							.initialTokens(rateLimitMetadata.tokensRemaining)
							.build()
							)
					.build(),
				TokensInheritanceStrategy.RESET);
	}


	private void evaluateRateLimitCost(Response returned, long tokens) {
		RateLimit rateLimit = returned.getObject(RateLimit.class, "rateLimit");
		if(rateLimit.cost!=tokens) {
			StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
			// 0 is getStackTrace
			// 1 is this method,
			// 2 is updateBucketConfiguration
			// 3 is executeSync
			// 4 is caller method where tokens count is defined
			StackTraceElement callerMethod = stackTraceElements[4];
			Log.errorf("The query defined in %s doesn't consume %d tokens, but %d", callerMethod, tokens, rateLimit.cost);
		}
	}

    @Retry
    public int getTodayCountForStargazers(String owner, String name) {
        return getTodayCountFor(owner, name, githubStarsToday, RepositoryWithStargazerCountToday.class);
    }

    @Retry
    public int getTodayCountForForks(String owner, String name) {
        return getTodayCountFor(owner, name, githubForksToday, RepositoryWithForkCountToday.class);
    }

    @Retry
    public int getTodayCountForIssues(String owner, String name) {
        return getTodayCountFor(owner, name, githubIssuesToday, RepositoryWithIssueCountToday.class);
    }

    @Retry
    public void getHistoryCountForStargazers(String owner, String name, boolean force, Function<RepositoryWithStargazerCountHistory, Boolean> processIndicator) {
        getHistoryCountFor(owner, name, githubStarsHistory, force, RepositoryWithStargazerCountHistory.class, processIndicator, repositoryPage -> repositoryPage == null || repositoryPage.stargazers() == null || repositoryPage.stargazers().pageInfo() == null);
    }

    @Retry
    public void getHistoryCountForForks(String owner, String name, boolean force, Function<RepositoryWithForkCountHistory, Boolean> processIndicator) {
        getHistoryCountFor(owner, name, githubForksHistory, force, RepositoryWithForkCountHistory.class, processIndicator, repositoryPage -> repositoryPage == null || repositoryPage.forks() == null || repositoryPage.forks().pageInfo() == null);
    }

    @Retry
    public void getHistoryCountForIssues(String owner, String name, boolean force, Function<RepositoryWithIssueCountHistory, Boolean> processIndicator) {
        getHistoryCountFor(owner, name, githubIssuesHistory, force, RepositoryWithIssueCountHistory.class, processIndicator, repositoryPage -> repositoryPage == null || repositoryPage.issues() == null || repositoryPage.issues().pageInfo() == null);
    }

    /**
     * Get total number of the indicator as of today
     *
     * @param owner the owner of the repository
     * @param name  the name of the repository
     * @return number of the indicator
     */
    private <T extends TodayCount> int getTodayCountFor(String owner, String name, String graphqlQueryToUse, Class<T> pageClass) {
        try {
            Map<String, Object> arguments = Map.of(
                    "owner", owner,
                    "name", name);
            Response response = executeSync(graphqlQueryToUse, arguments, 1);
            if (response.hasData() && (response.getErrors() == null || response.getErrors().isEmpty())) {
                T repo = response.getObject(pageClass, "repository");
                if (repo != null) {
                    return repo.getCount();
                } else {
                    Log.warnf("The GraphQL response for getTodayCountFor(%s, %s, %s) does not contain a 'repository' field. Response: %s\"", owner, name, pageClass.getSimpleName(), response.getData());
                    return 0;
                }
            } else {
                throw processGraphqlErrors(graphqlQueryToUse, arguments, response);
            }
        } catch (InvalidResponseException | ExecutionException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(String.format("Error retrieving %s count for %s/%s", pageClass.getSimpleName(), owner, name), e);
        }
    }

    /**
     * Execute all the requests required to have the whole indicator set.
     * So this will run a bunch of requests and process all their results.
     *
     * @param owner            the repository owner
     * @param name             the repository name
     * @param graphqlQueryToUse     the GraphQL query to use
     * @param force            force to download again the indicator
     * @param pageClass        the classof the pageable to use
     * @param processIndicator function that will process all received indicators
     *                         and return true if we must continue (implementation will usually return true if
     *                         at least one was persisted)
     * @param isNullPage       predicate to check if the page is null (ex: no repository found)
     */
    private <T extends PageableHistory> void getHistoryCountFor(String owner, String name, String graphqlQueryToUse, boolean force, Class<T> pageClass, Function<T, Boolean> processIndicator, Predicate<T> isNullPage) {
        try {
            Map<String, Object> arguments = new TreeMap<>(Map.of(
                    "owner", owner,
                    "name", name));
            T repositoryPage;
            boolean shouldContinue;
            do {
                Log.debugf("Fetching %s page for %s/%s with arguments: %s", owner, name, arguments);
                Response response = executeSync(graphqlQueryToUse, arguments, 1);
                if (response.hasData() && (response.getErrors() == null || response.getErrors().isEmpty())) {
                    repositoryPage = response.getObject(pageClass, "repository");
                    if (isNullPage.test(repositoryPage)) {
                        Log.errorf("Invalid or incomplete response from GraphQL for getHistoryFor(%s, %s, %s), arguments: %s. Response: %s", owner, name, pageClass.getSimpleName(), arguments, response.getData());
                        throw new RuntimeException(String.format("Incomplete GraphQL response for %s history.", pageClass.getSimpleName()));
                    }

                    shouldContinue = repositoryPage.hasPreviousPage();
                    boolean hasProcessedSomething = processIndicator.apply(repositoryPage);

                    if (!force && !hasProcessedSomething) {
                        Log.infof("No new %s processed for %s/%s in this page, early shutdown.", pageClass.getSimpleName(), owner, name);
                        shouldContinue = false;
                    }

                    if (shouldContinue) {
                        Log.debugf("Processing %s page for %s/%s. Next page to fetch before: %s", pageClass.getSimpleName(), owner, name, repositoryPage.startCursor());
                        arguments.put("before", repositoryPage.startCursor());
                    }
                } else {
                    Log.debugf("%s processing complete for %s/%s.", pageClass.getSimpleName(), owner, name);
                    throw processGraphqlErrors(graphqlQueryToUse, arguments, response);
                }
            } while (shouldContinue);
        } catch (InvalidResponseException | ExecutionException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(String.format("Error retrieving %s history for %s/%s", pageClass.getSimpleName(), owner, name), e);
        }

    }

	public boolean canComputeIndicator() {
		return rateLimitingBucket.getAvailableTokens()>TOKEN_LOWER_BOUND;
	}

}
