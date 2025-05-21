package org.ndx.aadarchi.technology.detector.indicators.github.graphql;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.ndx.aadarchi.technology.detector.indicators.github.graphql.entities.RepositoryWithForkCount;
import org.ndx.aadarchi.technology.detector.indicators.github.graphql.entities.RepositoryWithForkList;
import org.ndx.aadarchi.technology.detector.indicators.github.graphql.entities.RepositoryWithStargazerCount;
import org.ndx.aadarchi.technology.detector.indicators.github.graphql.entities.RepositoryWithStargazerList;

import graphql.GraphQLError;
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

	public class BucketThreadParkedLogger implements BucketListener {

		@Override
		public void onConsumed(long tokens) {}

		@Override
		public void onRejected(long tokens) {}

		@Override
		public void beforeParking(long nanos) {
			LocalDateTime until = LocalDateTime.now().plusNanos(nanos);
			Log.warnf("Thread will be parked until %s due to bucket having only %d tokens remaining",
					until,
					rateLimitingBucket.getAvailableTokens());
		}

		@Override
		public void onParked(long nanos) {
			Log.warnf("Thread was parked %s due to bucket having only %d tokens remaining. Operations resume NOW!",
					DurationFormatUtils.formatDurationHMS(TimeUnit.NANOSECONDS.toMillis(nanos)),
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



	private RuntimeException processGraphqlErrors(Map<String, Object> arguments, Response response) {
		return new GitHubGraphqlException(
				String.format(
						"Request\n"
						+ "%s\n"
						+ "when executed with parameters %s\n"
						+ "generated errors\n"
						+ "%s", 
						githubStarsToday,
						arguments,
						response.getErrors().stream()
							.map((error -> String.format("\t%s", 
									error.getMessage()))
						).collect(Collectors.joining("\n"))));
	}


	private Response executeSync(String query, Map<String, Object> arguments, long tokens) throws ExecutionException, InterruptedException {
		rateLimitingBucket.asBlocking().consume(tokens);
		Response returned = dynamicClient.executeSync(query, arguments);
		updateBucketConfiguration(returned);
		return returned;
	}

	private void updateBucketConfiguration(Response returned) {
		Map<String, String> metadata = returned.getTransportMeta()
				.entrySet()
				.stream()
				.collect(Collectors.toMap(
						entry -> entry.getKey().toLowerCase(),
						entry -> entry.getValue().stream().collect(Collectors.joining())));
		int tokensPerHour = Integer.parseInt(metadata.get("x-ratelimit-limit"));
		int tokensRemaining = Integer.parseInt(metadata.get("x-ratelimit-remaining"));
		int tokensUsed = Integer.parseInt(metadata.get("x-ratelimit-used"));
		int tokensResetInstant = Integer.parseInt(metadata.get("x-ratelimit-reset"));
		Instant resetInstant = Instant.ofEpochSecond(tokensResetInstant);
		if(tokensRemaining<100) {
			Log.warnf("%s tokens remaining. Bucket refulling will happen at %s",
					tokensRemaining,
					resetInstant.atZone(ZoneId.systemDefault())
					);
		} else {
			Log.debugf("%s tokens remaining locally, and %s tokens remaining on GitHub side.",
					rateLimitingBucket.getAvailableTokens(),
					tokensRemaining);
		}
		rateLimitingBucket.replaceConfiguration(
				BucketConfiguration.builder()
					.addLimit(BandwidthBuilder
							.builder()
							.capacity(tokensPerHour)
							.refillIntervallyAligned(tokensPerHour, Duration.ofHours(1), resetInstant)
							.initialTokens(tokensRemaining)
							.build()
							)
					.build(),
				TokensInheritanceStrategy.RESET);
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
				Response response = executeSync(githubStarsHistory, arguments, 100);
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
			Response response = executeSync(githubForksToday, arguments, 100);
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
				Response response = executeSync(githubForksHistory, arguments, 100);
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

}
