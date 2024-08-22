package org.ndx.aadarchi.technology.detector.npmjs;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.ndx.aadarchi.technology.detector.helper.ArtifactLoader;
import org.ndx.aadarchi.technology.detector.helper.FileHelper;
import org.ndx.aadarchi.technology.detector.helper.InterestingArtifactsDetailsDownloader;
import org.ndx.aadarchi.technology.detector.helper.NoContext;
import org.ndx.aadarchi.technology.detector.model.ArtifactDetails;
import org.ndx.aadarchi.technology.detector.model.ArtifactDetailsBuilder;

import com.github.fge.lambdas.Throwing;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import dev.failsafe.Failsafe;
import dev.failsafe.FailsafeExecutor;
import dev.failsafe.RateLimitExceededException;
import dev.failsafe.RateLimiter;
import dev.failsafe.RetryPolicy;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "ExtractPopularNpmjsArtifacts", mixinStandardHelpOptions = true, version = "ExtractPopularNpmjsArtifacts 0.1",
        description = "ExtractPopularNpmjsArtifacts made with jbang")
public
class ExtractPopularNpmjsArtifacts extends InterestingArtifactsDetailsDownloader<NoContext> {
	public static final Logger logger = Logger.getLogger(ExtractPopularNpmjsArtifacts.class.getName());
	/**
	 * @see https://github.com/npm/registry/blob/main/docs/download-counts.md
	 */
	private static final String DOWNLOADS_LAST_MONTH = "https://api.npmjs.org/downloads/point/%s/%s";

	public static void main(String... args) {
        int exitCode = new CommandLine(new ExtractPopularNpmjsArtifacts()).execute(args);
        System.exit(exitCode);
    }

	private FailsafeExecutor<Object> failsafe;

	@Override
	public Integer call() throws Exception {
		super.doCall(new NoContext());
		return 0;
	}
	

	@Override
	protected Collection<ArtifactDetails> injectDownloadInfosFor(NoContext context, Collection<ArtifactDetails> allDetails) {
		try {
			String period = "last-month";
			allDetails = getAllDownloadsForPeriod(allDetails, period);
			return allDetails;
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected HistoryBuilder createHistoryBuilder() {
		return new HistoryBuilder(this, gitHistory, getCache());
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
    		.filter(artifact -> artifact.getDownloads()>0)
			.sorted()
    		.collect(Collectors.toList());
	}
    
    private static class DownloadCount {
    	  long downloads;
    	  String start;
    	  String end;
    	  @SerializedName("package")
    	  String packageName;
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
				DownloadCount jsonResponse = FileHelper.gson.fromJson(response.body(), new TypeToken<DownloadCount>() {});
				return ArtifactDetailsBuilder.toBuilder(artifact)
					.downloads(jsonResponse.downloads)
					.build();
			});
	}
	
	@Override
	public Path getCache() {
		return super.getCache().toAbsolutePath().resolve("npmjs");
	}

	@Override
	protected Collection<ArtifactLoader<? super NoContext>> getArtifactLoaderCollection(NoContext context) {
		return Arrays.asList( 
    			// Way to much complicated
//    			new CodebaseShowArtifacts(),
				new JavascriptTechEmpowerArtifacts(getCache(), techEmpowerFrameworks),
    			new PopularNpmArtifacts(getCache(), client)
    			);
	}
}
