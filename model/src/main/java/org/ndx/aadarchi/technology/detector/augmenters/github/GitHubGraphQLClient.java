package org.ndx.aadarchi.technology.detector.augmenters.github;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Logger;

import org.ndx.aadarchi.technology.detector.exceptions.ExtractionException;
import org.ndx.aadarchi.technology.detector.helper.FileHelper;
import org.ndx.aadarchi.technology.detector.helper.InterestingArtifactsDetailsDownloader;

import com.fasterxml.jackson.databind.JsonNode;

import dev.failsafe.Failsafe;
import dev.failsafe.FailsafeExecutor;
import dev.failsafe.RateLimiter;
import dev.failsafe.RetryPolicy;
import dev.failsafe.function.CheckedSupplier;

public class GitHubGraphQLClient {
	private static final Logger logger = Logger.getLogger(GitHubGraphQLClient.class.getName());
	public static String STARGAZERS_COUNT;
	public static String STARGAZERS_LIST;
	public static String REPOSITORY_ID;
	private Optional<Integer> limit = Optional.empty();
	private Optional<Integer> remaining = Optional.empty();
	private Optional<Date> resetAt = Optional.empty();
	private FailsafeExecutor<Object> failsafe = Failsafe.with(RetryPolicy.builder().withMaxRetries(3).build());
	private Optional<RateLimiter<Object>> rateLimiter = Optional.empty();

	static {
		Properties properties = new Properties();
		try {
			try (InputStream input = AddGitHubStarsAtPeriod.class.getClassLoader().getResourceAsStream("github.xml")) {
				properties.loadFromXML(input);
			}
			REPOSITORY_ID = properties.getProperty("repository_id");
			STARGAZERS_LIST = properties.getProperty("stargazers_list");
			STARGAZERS_COUNT = properties.getProperty("stargazers_count");
		} catch (IOException e) {
			throw new Error("Cannot load github.xml file from CLASSPATH", e);
		}
	}

	JsonNode runGraphQlRequest(String githubToken, String requestText) {
		try {
			HttpRequest request = createGraphQLQuery(githubToken, requestText);
			HttpClient client = InterestingArtifactsDetailsDownloader.client;
			HttpResponse<String> response = failsafe.get(sendRequest(request, client));
			// Analyze headers to get available quota
			updateHeaders(response);
			if (response.statusCode() < 300) {
				return FileHelper.getObjectMapper().readTree(response.body());
			} else {
				throw new UnsupportedOperationException(
						String.format("Seems like HTTP request was incorrect (request was %s)(response was %s)",
								request.bodyPublisher().get(), response.body()));
			}
		} catch (IOException e) {
			throw new CannotPerformGraphQL(String.format(
					"Unable to perform graph query\n" + "=====\n" + "%s\n" + "=====\n" + "due to lower level exception",
					requestText), e);
		}
	}
	
	private static class CannotPerformHttpRequest extends ExtractionException {

		public CannotPerformHttpRequest(String message, Throwable cause) {
			super(message, cause);
		}
		
	}

	private CheckedSupplier<HttpResponse<String>> sendRequest(HttpRequest request, HttpClient client) {
		return () -> {
			try {
				return client.send(request, BodyHandlers.ofString());
			} catch(Exception e) {
				throw new CannotPerformHttpRequest("Something went wrong, no ?",e);
			}
		};
	}

	/**
	 * Update local configuration from http response headers. This mainly sets
	 * correct rate limit (and update failsafe accordingly)
	 * 
	 * @see https://docs.github.com/en/graphql/overview/rate-limits-and-node-limits-for-the-graphql-api#staying-under-the-rate-limit
	 * @param response
	 */
	private void updateHeaders(HttpResponse<String> response) {
		HttpHeaders headers = response.headers();
		if (limit.isEmpty()) {
			limit = Optional
					.ofNullable(headers.firstValue("x-ratelimit-limit").map(Integer::parseInt).orElseGet(() -> null));
		}
		remaining = Optional
				.ofNullable(headers.firstValue("x-ratelimit-remaining").map(Integer::parseInt).orElseGet(() -> null));
		resetAt = Optional.ofNullable(headers.firstValue("x-ratelimit-reset").map(Long::parseLong)
				.map(date -> date * 1000).map(date -> new Date(date)).orElseGet(() -> null));
		urgencyBrake();
		if (limit.isPresent() && remaining.isPresent() && resetAt.isPresent()) {
			Duration delay = delay();
			updateFailsafe(limit.get(), remaining.get(), delay);
		}
	}

	private Duration delay() {
		Date resetAtDate = resetAt.get();
		Date now = new Date();
		Duration delay = Duration.between(now.toInstant().atZone(ZoneOffset.UTC),
				resetAtDate.toInstant().atZone(ZoneOffset.UTC));
		return delay;
	}

	private void urgencyBrake() {
		if (remaining.isPresent() && resetAt.isPresent()) {
			if (remaining.get() <= 1000) {
				Duration delay = delay();
				long waitDelay = Math.abs(delay.toMillis());
				logger.warning(
						String.format("We're dangerously approaching our limits, waiting %d minutes %02d seconds",
								delay.toMinutesPart(), delay.toSecondsPart())
						);
				while (waitDelay > 0) {
					synchronized (this) {
						try {
							long waitStep = Math.min(5 * 60 * 1000, waitDelay);
							long waitSeconds = waitStep/1000;
							logger.warning(String.format("Waiting %d minutes %02d seconds", 
									waitSeconds/60, 
									waitSeconds%60));
							this.wait(waitStep);
							waitDelay -= waitStep;
						} catch (InterruptedException e) {
							logger.severe("Seems like http wait has been interrupted. THis may trigger weird bugs.");
						}
					}
				}
			}
		}
	}

	private void updateFailsafe(Integer limit, Integer remaining, Duration delay) {
		if (rateLimiter.isEmpty()) {
			rateLimiter = Optional.of(RateLimiter
					.smoothBuilder(limit, delay)
					.withMaxWaitTime(Duration.of(1L, ChronoUnit.MINUTES))
					.build());
			failsafe = Failsafe.with(RetryPolicy.builder().withMaxRetries(3).build(), rateLimiter.get());
		}
	}

	HttpRequest createGraphQLQuery(String githubToken, String query) {
		String jsonQuery = String.format("{ \"query\": \"%s\" }",
				// We have to embed query in JSON.
				// For that
				query
						// Quote line returns
						.replace("\n", "")
						// Quote double quotes
						.replace("\"", "\\\""));
		HttpRequest request = HttpRequest.newBuilder().uri(URI.create("https://api.github.com/graphql"))
				.header("Authorization", String.format("Bearer %s", githubToken))
				.header("Content-Type", "application/json; charset=utf-8").POST(BodyPublishers.ofString(jsonQuery))
				.build();
		return request;
	}

	private String token;

	private GitHubGraphQLClient(String githubToken) {
		this.token = githubToken;
	}

	public JsonNode runGraphQLQuery(String template, Object... arguments) {
		String text = String.format(template, arguments);
		return runGraphQlRequest(token, text);
	}

	private static Optional<GitHubGraphQLClient> client = Optional.empty();

	public static GitHubGraphQLClient getClient(String githubToken) {
		if (client.isEmpty()) {
			client = Optional.ofNullable(new GitHubGraphQLClient(githubToken));
		}
		return client.get();
	}
}
