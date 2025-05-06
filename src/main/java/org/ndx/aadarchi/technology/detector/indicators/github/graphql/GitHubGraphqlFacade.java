package org.ndx.aadarchi.technology.detector.indicators.github.graphql;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.faulttolerance.Retry;

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
	
	public class BucketThreadParkedLogger implements BucketListener {

		@Override
		public void onConsumed(long tokens) {}

		@Override
		public void onRejected(long tokens) {}

		@Override
		public void onParked(long nanos) {
			Log.warnf("Thread was parked %s due to bucket having only %d tokens remaining",
					DurationFormatUtils.formatDurationHMS(nanos/1000),
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
				return response.getObject(StargazerCountRepository.class, "repository").stargazerCount;
			} else {
				throw processGraphqlErrors(arguments, response);
			}
		} catch (InvalidResponseException | ExecutionException | InterruptedException e) {
			throw new RuntimeException("TODO handle Exception", e);
		}
	}

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
	public void getAllStargazers(String owner, String name, boolean force, Function<StargazerListRepository, Boolean> processStargazers) {
		try {
			Map<String, Object> arguments = new TreeMap<>(Map.of(
					"owner", owner,
					"name", name));
			StargazerListRepository repositoryPage = null;
			boolean shouldContinue = true;
			do {
				Response response = executeSync(githubStarsHistory, arguments, 100);
				if(response.getErrors()==null || response.getErrors().isEmpty()) {
					repositoryPage = response.getObject(StargazerListRepository.class, "repository");
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

	private Response executeSync(String query, Map<String, Object> arguments, long tokens) throws ExecutionException, InterruptedException {
		rateLimitingBucket.asBlocking().consume(tokens);
		Response returned = dynamicClient.executeSync(query, arguments);
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
		LocalDateTime resetInstant = LocalDateTime.ofEpochSecond(tokensResetInstant, 0, OffsetDateTime.now().getOffset());
		if(tokensRemaining<100) {
			Log.warnf("%s tokens remaining. Bucket refulling will happen at %s",
					tokensRemaining,
					resetInstant);
		} else {
			Log.debugf("%s tokens remaining locally, and %s tokens remaining on GitHub side.",
					rateLimitingBucket.getAvailableTokens(), 
					tokensRemaining);
		}
		rateLimitingBucket.replaceConfiguration(
				BucketConfiguration.builder()
					.addLimit(limit ->
						limit.capacity(tokensRemaining)
							.refillIntervallyAligned(tokensPerHour, Duration.ofHours(1), resetInstant.toInstant(ZoneOffset.UTC))
					)
					.build(), 
				TokensInheritanceStrategy.RESET);
		return returned;
	}
}
