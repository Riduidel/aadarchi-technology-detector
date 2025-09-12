package org.ndx.aadarchi.technology.detector.indicators.github.graphql;

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
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.ndx.aadarchi.technology.detector.indicators.github.graphql.entities.RateLimit;
import org.ndx.aadarchi.technology.detector.indicators.github.graphql.entities.RepositoryWithForkCount;
import org.ndx.aadarchi.technology.detector.indicators.github.graphql.entities.RepositoryWithForkList;
import org.ndx.aadarchi.technology.detector.indicators.github.graphql.entities.RepositoryWithIssueCount;
import org.ndx.aadarchi.technology.detector.indicators.github.graphql.entities.RepositoryWithIssueList;
import org.ndx.aadarchi.technology.detector.indicators.github.graphql.entities.RepositoryWithStargazerCount;
import org.ndx.aadarchi.technology.detector.indicators.github.graphql.entities.RepositoryWithStargazerList;

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
import java.util.stream.Collectors;

@ApplicationScoped
public class GitHubGraphqlFacade {
	private static final int TOKEN_LOWER_BOUND = 100;

	@Inject
	@GraphQLClient("github")
	DynamicGraphQLClient dynamicClient;
	
	@ConfigProperty(name = "tech-trends.indicators.github.stars.graphql.today")
	String githubStarsToday;
	@ConfigProperty(name = "tech-trends.indicators.github.stars.graphql.history")
	String githubStarsHistory;
	@ConfigProperty(name = "tech-trends.indicators.github.forks.graphql.today")
	String githubForksToday;
	@ConfigProperty(name = "tech-trends.indicators.github.forks.graphql.history")
	String githubForksHistory;
    @ConfigProperty(name = "tech-trends.indicators.github.issues.graphql.today")
    String githubIssuesToday;
    @ConfigProperty(name = "tech-trends.indicators.github.issues.graphql.history")
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



	private GitHubGraphqlException processGraphqlErrors(Map<String, Object> arguments, Response response) {
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
				githubStarsToday,
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

	/**
	 * Get total number of stargazers as of today
	 * @param owner
	 * @param name
	 * @return number of stargazers
	 * @throws InterruptedException 
	 * @throws ExecutionException 
	 */
	@Retry(maxRetries = 3)
	public int getStargazers(String owner, String name) {
		try {
			Map<String, Object> arguments = Map.of(
					"owner", owner,
					"name", name);
			Response response = executeSync(githubStarsToday, arguments, 1);
			if(response.getErrors()==null || response.getErrors().isEmpty()) {
				return response.getObject(RepositoryWithStargazerCount.class, "repository").stargazerCount;
			} else {
				throw processGraphqlErrors(arguments, response);
			}
		} catch (InvalidResponseException | ExecutionException | InterruptedException e) {
			throw new RuntimeException("TODO handle Exception", e);
		}
	}
	/**
	 * Execute all the requests required to have the whole stargazers set.
	 * So this will run a bunch of requests and process all their results.
	 * @param owner
	 * @param name
	 * @param processStargazers function that will process all received stargazers 
	 * and return true if we must continue (implementation will usually return true if 
	 * at least one was persisted)
	 */
	@Retry(maxRetries = 3)
	public void getAllStargazers(String owner, String name, boolean force, Function<RepositoryWithStargazerList, Boolean> processStargazers) {
		try {
			Map<String, Object> arguments = new TreeMap<>(Map.of(
					"owner", owner,
					"name", name));
			RepositoryWithStargazerList repositoryPage = null;
			boolean shouldContinue = true;
			do {
				Response response = executeSync(githubStarsHistory, arguments, 1);
				if(response.getErrors()==null || response.getErrors().isEmpty()) {
					repositoryPage = response.getObject(RepositoryWithStargazerList.class, "repository");
					shouldContinue = repositoryPage.stargazers.pageInfo.hasPreviousPage;
					boolean hasSavedSomething = processStargazers.apply(repositoryPage);
					if(!force) {
						if(hasSavedSomething) {
						} else {
							Log.infof("We had no new stargazer of %s/%s in this result, stopping", owner, name);
							shouldContinue = false;
						}
					}
					arguments.put("before", repositoryPage.stargazers.pageInfo.startCursor);
				} else {
					throw processGraphqlErrors(arguments, response);
				}
			} while(shouldContinue);
		} catch (InvalidResponseException | ExecutionException | InterruptedException e) {
			throw new RuntimeException("TODO handle Exception", e);
		}
		
	}

	/**
	 * Retrieves the current total number of forks for a repository.
	 * @param owner Repository owner
	 * @param name Repository name
	 * @return Total number of forks
	 */
	public int getCurrentTotalNumberOfFork(String owner, String name) {
		try {
			Map<String, Object> arguments = Map.of(
					"owner", owner,
					"name", name);
			Response response = executeSync(githubForksToday, arguments, 1);
			if(response.hasData() && (response.getErrors() == null || response.getErrors().isEmpty())) {
				RepositoryWithForkCount repo = response.getObject(RepositoryWithForkCount.class, "repository");
				if (repo != null) {
					return repo.forkCount;
				} else {
					Log.warnf("The GraphQL response for getForkCount(%s, %s) does not contain a 'repository' field. Response: %s\"", owner, name, response.getData());
					return 0;
				}
			} else {
				throw processGraphqlErrors(arguments, response);
			}
		} catch (InvalidResponseException | ExecutionException | InterruptedException e) {
			throw new RuntimeException(String.format("Error retrieving fork count for %s/%s", owner, name), e);
		}
	}

	/**
	 * Retrieves the complete fork history for a repository, page by page.
	 * @param owner Repository owner
	 * @param name Repository name
	 * @param force If true, continues even if a page contains no new processed data.
	 * @param processForks Function to process each received fork page. Must return true if processing should continue.
	 */
	public void getAllForks(String owner, String name, boolean force, Function<RepositoryWithForkList, Boolean> processForks) {
		try {
			Map<String, Object> arguments = new TreeMap<>(Map.of(
					"owner", owner,
					"name", name));
			RepositoryWithForkList repositoryPage;
			boolean shouldContinue = true;
			do {
				Log.debugf("Fetching forks page for %s/%s with arguments: %s", owner, name, arguments);
				Response response = executeSync(githubForksHistory, arguments, 1);
				if(response.hasData() && (response.getErrors() == null || response.getErrors().isEmpty())) {
					repositoryPage = response.getObject(RepositoryWithForkList.class, "repository");
					if (repositoryPage == null || repositoryPage.forks == null || repositoryPage.forks.pageInfo == null) {
						Log.errorf("Invalid or incomplete response from GraphQL for getAllForks(%s, %s), arguments: %s. Response: %s", owner, name, arguments, response.getData());
						throw new RuntimeException("Incomplete GraphQL response for fork history.");
					}

					shouldContinue = repositoryPage.forks.pageInfo.hasPreviousPage;
					boolean hasProcessedSomething = processForks.apply(repositoryPage);

					if(!force && !hasProcessedSomething) {
						Log.infof("No new forks processed for %s/%s in this page, early shutdown.", owner, name);
						shouldContinue = false;
					}

					if (shouldContinue) {
						Log.debugf("Processing fork page for %s/%s. Next page to fetch before: %s", owner, name, repositoryPage.forks.pageInfo.startCursor);
						arguments.put("before", repositoryPage.forks.pageInfo.startCursor);
					}
				} else {
					Log.debugf("Fork processing complete for %s/%s.", owner, name);
					throw processGraphqlErrors(arguments, response);
				}
			} while(shouldContinue);
		} catch (InvalidResponseException | ExecutionException | InterruptedException e) {
			throw new RuntimeException(String.format("Error retrieving fork history for %s/%s", owner, name), e);
		}
	}

	public boolean canComputeIndicator() {
		return rateLimitingBucket.getAvailableTokens()>TOKEN_LOWER_BOUND;
	}

    /**
     * Retrieves the current total number of issues for a repository.
     * @param owner Repository owner
     * @param name Repository name
     * @return Total number of issues
     */
    public int getCurrentTotalNumberOfIssue(String owner, String name) {
        try {
            Map<String, Object> arguments = Map.of(
                    "owner", owner,
                    "name", name);
            Response response = executeSync(githubIssuesToday, arguments, 1);
            if(response.hasData() && (response.getErrors() == null || response.getErrors().isEmpty())) {
                RepositoryWithIssueCount repo = response.getObject(RepositoryWithIssueCount.class, "repository");
                if (repo != null) {
                    return repo.issues.totalCount;
                } else {
                    Log.warnf("The GraphQL response for getIssueCount(%s, %s) does not contain a 'repository' field. Response: %s\"", owner, name, response.getData());
                    return 0;
                }
            } else {
                throw processGraphqlErrors(arguments, response);
            }
        } catch (InvalidResponseException | ExecutionException | InterruptedException e) {
            throw new RuntimeException(String.format("Error retrieving issue count for %s/%s", owner, name), e);
        }
    }

    /**
     * Retrieves the complete issue history for a repository, page by page.
     * @param owner Repository owner
     * @param name Repository name
     * @param force If true, continues even if a page contains no new processed data.
     * @param processIssues Function to process each received issue page. Must return true if processing should continue.
     */
    public void getAllIssues(String owner, String name, boolean force, Function<RepositoryWithIssueList, Boolean> processIssues) {
        try {
            Map<String, Object> arguments = new TreeMap<>(Map.of(
                    "owner", owner,
                    "name", name));
            RepositoryWithIssueList repositoryPage;
            boolean shouldContinue = true;
            do {
                Log.debugf("Fetching issues page for %s/%s with arguments: %s", owner, name, arguments);
                Response response = executeSync(githubIssuesHistory, arguments, 1);
                if(response.hasData() && (response.getErrors() == null || response.getErrors().isEmpty())) {
                    repositoryPage = response.getObject(RepositoryWithIssueList.class, "repository");
                    if (repositoryPage == null || repositoryPage.issues == null || repositoryPage.issues.pageInfo == null) {
                        Log.errorf("Invalid or incomplete response from GraphQL for getAllIssues(%s, %s), arguments: %s. Response: %s", owner, name, arguments, response.getData());
                        throw new RuntimeException("Incomplete GraphQL response for issue history.");
                    }

                    shouldContinue = repositoryPage.issues.pageInfo.hasPreviousPage;
                    boolean hasProcessedSomething = processIssues.apply(repositoryPage);

                    if(!force && !hasProcessedSomething) {
                        Log.infof("No new issues processed for %s/%s in this page, early shutdown.", owner, name);
                        shouldContinue = false;
                    }

                    if (shouldContinue) {
                        Log.debugf("Processing issue page for %s/%s. Next page to fetch before: %s", owner, name, repositoryPage.issues.pageInfo.startCursor);
                        arguments.put("before", repositoryPage.issues.pageInfo.startCursor);
                    }
                } else {
                    Log.debugf("Issue processing complete for %s/%s.", owner, name);
                    throw processGraphqlErrors(arguments, response);
                }
            } while(shouldContinue);
        } catch (InvalidResponseException | ExecutionException | InterruptedException e) {
            throw new RuntimeException(String.format("Error retrieving issue history for %s/%s", owner, name), e);
        }
    }
}
