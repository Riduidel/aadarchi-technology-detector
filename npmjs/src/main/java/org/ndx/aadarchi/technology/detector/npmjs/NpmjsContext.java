package org.ndx.aadarchi.technology.detector.npmjs;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.ndx.aadarchi.technology.detector.helper.FileHelper;
import org.ndx.aadarchi.technology.detector.helper.Utils;
import org.ndx.aadarchi.technology.detector.loader.DetailsFetchingContext;
import org.ndx.aadarchi.technology.detector.model.ArtifactDetails;
import org.ndx.aadarchi.technology.detector.model.ArtifactDetailsBuilder;
import org.ndx.aadarchi.technology.detector.npmjs.model.DownloadCount;
import org.ndx.aadarchi.technology.detector.npmjs.model.Package;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.fge.lambdas.Throwing;

import dev.failsafe.Failsafe;
import dev.failsafe.FailsafeExecutor;
import dev.failsafe.RateLimitExceededException;
import dev.failsafe.RateLimiter;
import dev.failsafe.RetryPolicy;

public class NpmjsContext implements DetailsFetchingContext {
	public static final Logger logger = Logger.getLogger(NpmjsContext.class.getName());
	/**
	 * @see https://github.com/npm/registry/blob/main/docs/download-counts.md
	 */
	private static final String DOWNLOADS_LAST_MONTH = "https://api.npmjs.org/downloads/point/%s/%s";
	private static final String REGISTRY = "https://registry.npmjs.org/%s";

	private FailsafeExecutor<Object> failsafe;
	private HttpClient client;
    public NpmjsContext(HttpClient client) {
    	this.client = client;
	}
	/**
	 * Get all downloads on a given period, as defined by npmjs download api
	 * @param artifactsToQuery
	 * @param period the period of time to get downloads for
	 * @return list of artifacts having at least one download on this period
	 * @throws IOException
	 */
	Collection<ArtifactDetails> getAllDownloadsForPeriod(Collection<ArtifactDetails> artifactsToQuery, String period) throws IOException {
		// Now get download count for last month
    	return artifactsToQuery.stream()
    		// Do not use parallel, cause the download count api is quite cautious on load and will fast put an hauld on our queries
//    		.parallel()
    		.map(Throwing.function(artifact -> countDownloadsOf(artifact, period)))
    		.filter(artifact -> artifact.getDownloads()!=null)
    		.filter(artifact -> artifact.getDownloads()>0)
			.sorted()
    		.collect(Collectors.toList());
	}
    
    private FailsafeExecutor<Object> getFailsafeExecutorForDownloads() {
		if(failsafe==null) {
			RateLimiter<Object> limiter = RateLimiter.smoothBuilder(1, Duration.ofSeconds(1)).build();
			RetryPolicy<Object> retryOnLimitReached = RetryPolicy.builder()
					.handle(RateLimitExceededException.class)
					.withDelay(Duration.ofSeconds(1))
					.withMaxRetries(-1)
					.build();
			RetryPolicy<Object> retryOnFailure = RetryPolicy.builder()
				.handle(IOException.class, InterruptedException.class)
				  .onFailedAttempt(e -> logger.log(Level.SEVERE, "Connection attempt failed", e.getLastException()))
				  .onRetry(e -> logger.warning( String.format( "Failure #%s. Retrying.", e.getAttemptCount())))
				.withDelay(Duration.ofSeconds(10))
				.withMaxRetries(3)
				.build();
			failsafe = Failsafe.with(retryOnLimitReached, limiter, retryOnFailure);
		}
		return failsafe;
	}
    /**
     * Create an artifact with download count for the given artifact
     * @param artifact
     * @return
     * @throws InterruptedException 
     * @throws IOException 
     */
	private ArtifactDetails countDownloadsOf(ArtifactDetails artifact, String period) throws IOException, InterruptedException {
		logger.info(String.format("Getting downloads counts of %s for period %s", 
				artifact.getName(), period));
		return getFailsafeExecutorForDownloads()
			.get(() -> {
				HttpRequest request = HttpRequest.newBuilder(URI.create(
						String.format(DOWNLOADS_LAST_MONTH, period, artifact.getName()))).build();
				HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
				DownloadCount jsonResponse = FileHelper.getObjectMapper().readValue(response.body(), new TypeReference<DownloadCount>() {});
				return ArtifactDetailsBuilder.toBuilder(artifact)
					.downloads(jsonResponse.downloads)
					.build();
			});
	}

	@Override
	public ArtifactDetails addDetails(ArtifactDetails artifact) throws Exception {
		return getFailsafeExecutorForDownloads()
				.get(() -> {
					logger.info(String.format("Adding details to %s", artifact.getName()));
					HttpRequest request = HttpRequest.newBuilder(URI.create(
							String.format(REGISTRY, artifact.getName()))).build();
					HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
					Package jsonResponse = FileHelper.getObjectMapper().readValue(response.body(), new TypeReference<Package>() {});
					ArtifactDetailsBuilder builder = ArtifactDetailsBuilder.toBuilder(artifact);
					if(jsonResponse.description!=null)
						builder = builder.description(jsonResponse.description);
					if(jsonResponse.homepage!=null)
						builder = builder.urls(Map.of(Utils.getDomain(jsonResponse.homepage), jsonResponse.homepage));
					if(jsonResponse.keywords!=null)
						builder = builder.tags(new ArrayList<>(jsonResponse.keywords));
					if(jsonResponse.license!=null)
						builder = builder.licenses(Arrays.asList(jsonResponse.license));
					return builder
						.build();
				});
	}

}
