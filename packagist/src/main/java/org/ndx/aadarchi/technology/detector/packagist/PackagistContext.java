package org.ndx.aadarchi.technology.detector.packagist;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.fge.lambdas.Throwing;
import dev.failsafe.*;
import org.kohsuke.github.GitHub;
import org.ndx.aadarchi.technology.detector.helper.FileHelper;
import org.ndx.aadarchi.technology.detector.helper.Utils;
import org.ndx.aadarchi.technology.detector.loader.AbstractContext;
import org.ndx.aadarchi.technology.detector.loader.DetailsFetchingContext;
import org.ndx.aadarchi.technology.detector.model.ArtifactDetails;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.ndx.aadarchi.technology.detector.model.ArtifactDetailsBuilder;
import org.ndx.aadarchi.technology.detector.packagist.model.DownloadCount;
import org.ndx.aadarchi.technology.detector.packagist.model.Package;

public class PackagistContext extends AbstractContext implements DetailsFetchingContext {
    private static final Logger logger = Logger.getLogger(PackagistContext.class.getName());
    private static final String DOWNLOADS_LAST_MONTH = "https://packagist.org/packages/%s/stats.json";
    private static final String REGISTRY = "https://packagist.org/packages/%s.json";

    private FailsafeExecutor<Object> failsafe;
    private final HttpClient client;

    public PackagistContext(HttpClient client, Path cache, GitHub github) {
        super(cache, github);
        this.client = client;
    }

    Collection<ArtifactDetails> getAllDownloadsForLastMonth(Collection<ArtifactDetails> artifactsToQuery) throws IOException {
        // Now get download count for last month
        return artifactsToQuery.stream()
                .map(Throwing.function(artifact -> countDownloadsOf(artifact)))
                .filter(artifact -> artifact.getDownloads()!=null)
                .filter(artifact -> artifact.getDownloads()>0)
                .sorted()
                .collect(Collectors.toList());
    }

    private ArtifactDetails countDownloadsOf(ArtifactDetails artifact) throws IOException, InterruptedException {
        logger.info(String.format("Getting downloads counts of %s",
                artifact.getName()));
        return getFailsafeExecutorForDownloads()
                .get(() -> {
                    HttpRequest request = HttpRequest.newBuilder(URI.create(
                            String.format(DOWNLOADS_LAST_MONTH, artifact.getName()))).build();
                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    DownloadCount jsonResponse = FileHelper.getObjectMapper().readValue(response.body(), new TypeReference<DownloadCount>() {});
                    return ArtifactDetailsBuilder.toBuilder(artifact)
                            .downloads(jsonResponse.monthly)
                            .build();
                });
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

    @Override
    public ArtifactDetails addDetails(ArtifactDetails artifact) throws Exception {
        return getFailsafeExecutorForDownloads()
                .get(() -> {
                    logger.info(String.format("Adding details to %s", artifact.getName()));
                    HttpRequest request = HttpRequest.newBuilder(URI.create(
                            String.format(REGISTRY, artifact.getName()))).build();
                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
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
