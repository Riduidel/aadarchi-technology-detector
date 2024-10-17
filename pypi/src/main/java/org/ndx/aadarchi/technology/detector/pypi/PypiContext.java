package org.ndx.aadarchi.technology.detector.pypi;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.kohsuke.github.GitHub;
import org.ndx.aadarchi.technology.detector.helper.FileHelper;
import org.ndx.aadarchi.technology.detector.helper.Utils;
import org.ndx.aadarchi.technology.detector.loader.AbstractContext;
import org.ndx.aadarchi.technology.detector.loader.DetailsFetchingContext;
import org.ndx.aadarchi.technology.detector.model.ArtifactDetails;
import org.ndx.aadarchi.technology.detector.model.ArtifactDetailsBuilder;
import org.ndx.aadarchi.technology.detector.model.VersionDetails;
import org.ndx.aadarchi.technology.detector.model.VersionDetailsBuilder;

import com.fasterxml.jackson.core.JacksonException;

import dev.failsafe.Failsafe;
import dev.failsafe.FailsafeExecutor;
import dev.failsafe.RateLimitExceededException;
import dev.failsafe.RateLimiter;
import dev.failsafe.RetryPolicy;
import org.ndx.aadarchi.technology.detector.pypi.exception.PypiContentException;
import org.ndx.aadarchi.technology.detector.pypi.exception.PypiExtractionException;
import org.ndx.aadarchi.technology.detector.pypi.exception.PypiHttpException;
import org.ndx.aadarchi.technology.detector.pypi.exception.RateLimitReached;

public class PypiContext extends AbstractContext implements DetailsFetchingContext {
	public static final Logger logger = Logger.getLogger(PypiContext.class.getName());
	private HttpClient client;
	private transient FailsafeExecutor<Object> failsafe;
	private static final String INFOS = "https://pypi.org/pypi/%s/json";
	private static final String DOWNLOADS = "https://pypistats.org/api/packages/%s/recent";
	private static final String POPULAR = "https://hugovk.github.io/top-pypi-packages/top-pypi-packages-30-days.min.json";
	
	public PypiContext(HttpClient client, GitHub github, Path cache) {
		super(cache, github);
		this.client = client;
	}

	@Override
	public ArtifactDetails addDetails(ArtifactDetails source) throws Exception {
		if(source.getDescription()==null) {
			logger.info(String.format("Adding missing details to %s", source));
			String url = String.format(INFOS, source.getName());
			HttpRequest request = HttpRequest.newBuilder(URI.create(url))
					.header("Accept", "application/json")
					.build();
			HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
			if(response.statusCode()<300) {
				String text = response.body();
				Map content = FileHelper.getObjectMapper().readValue(text, Map.class);
				return buildArtifactDetails(source, content);
			} else {
				logger.severe(() -> String.format("Seems like getting infos for %s failed due to http error %s. Url was %s", 
						source, response.statusCode(), url));
			}
		}
		return source;
	}

	private ArtifactDetails buildArtifactDetails(ArtifactDetails source, Map content) {
		ArtifactDetailsBuilder builder = ArtifactDetailsBuilder.toBuilder(source);
		if(content.containsKey("info")) {
			Map info = (Map) content.get("info");
			builder = builder
				.categories((List<String>) info.get("classifiers"))
				.description((String) info.get("description"));
			builder = handleTags(builder, info);
			builder = handleLicenses(builder, info);
			builder = handleUrls(builder, info);
		}
		builder = handleVersions(content, builder);
		return builder.build();
	}

	private ArtifactDetailsBuilder handleVersions(Map content, ArtifactDetailsBuilder builder) {
		if(content.containsKey("releases")) {
			SortedMap<String, VersionDetails> versions = new TreeMap<String, VersionDetails>();
			Map releases = (Map) content.get("releases");
			for(Object key : releases.keySet()) {
				String version = (String) key;
				List files = (List) releases.get(key);
				VersionDetails details = null;
				VersionDetailsBuilder versionBuilder = VersionDetailsBuilder.versionDetails();
				Iterator filesIter = files.iterator();
				while(details==null && filesIter.hasNext()) {
					Map file = (Map) filesIter.next();
					if(file.containsKey("upload_time_iso_8601")) {
						versionBuilder = versionBuilder.date((String) file.get("upload_time_iso_8601"));
						details = versionBuilder.build();
					}
				}
				versions.put(version, details);
			}
			builder = builder.versions(versions);
		}
		return builder;
	}

	private ArtifactDetailsBuilder handleTags(ArtifactDetailsBuilder builder, Map info) {
		if(info.containsKey("keywords")) {
			String keywords = (String) info.get("keywords");
			if(keywords!=null && !keywords.isBlank())
				builder = builder.tags(Arrays.asList(keywords.split(",")));
		}
		return builder;
	}

	private ArtifactDetailsBuilder handleLicenses(ArtifactDetailsBuilder builder, Map info) {
		if(info.containsKey("license")) {
			builder = builder.licenses(Arrays.asList((String) info.get("license")));
		}
		return builder;
	}

	private ArtifactDetailsBuilder handleUrls(ArtifactDetailsBuilder builder, Map info) {
		if(info.containsKey("project_urls")) {
			Map<String, String> urls = (Map<String, String>) info.get("project_urls");
			if(urls==null || urls.isEmpty()) {
				Map<String, String> value = new TreeMap<String, String>();
				for(String key : Arrays.asList("project_url", "package_url")) {
					if(info.containsKey(key)) {
						String url = (String) info.get(key);
						value.put(Utils.getDomain(url), url);
					}
				}
				builder = builder.urls(value);
			} else {
				Map<String, String> value = urls.values().stream()
					.filter(v -> !"UNKNOWN".equals(v))
					.map(v -> Map.entry(Utils.getDomain(v), v))
					.collect(Collectors.toMap(
							Map.Entry<String, String>::getKey, 
							Map.Entry<String, String>::getValue,
							(a, b) -> a,
							() -> new TreeMap<String, String>()));
				builder = builder
						.urls(value);
			}
		}
		return builder;
	}

	public ArtifactDetails countDownloadsOf(ArtifactDetails artifact, String period) {
		logger.info(String.format("Getting downloads counts of %s for period %s", 
				artifact.getName(), period));
		FailsafeExecutor<Object> failsafe = getFailsafeExecutorForDownloads();
		return failsafe
				.get(() -> doCountDownloadsOf(artifact, period));
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
			RetryPolicy<Object> retryOnTooMuchRequests = RetryPolicy.builder()
					  .onFailedAttempt(e -> logger.log(Level.SEVERE, "Rate limit reached", e.getLastException()))
					  .onRetry(e -> logger.warning( String.format( "Failure #%s. Retrying.", e.getAttemptCount())))
					.handle(RateLimitReached.class)
					.withDelay(Duration.ofSeconds(10))
					.withMaxRetries(3)
					.build();
			failsafe = Failsafe.with(retryOnLimitReached, limiter, retryOnFailure, retryOnTooMuchRequests);
		}
		return failsafe;
	}

	private ArtifactDetails doCountDownloadsOf(ArtifactDetails artifact, String period) throws IOException, InterruptedException {
			String url = String.format(DOWNLOADS, artifact.getName().toLowerCase());
			HttpRequest request = HttpRequest.newBuilder(URI.create(url))
					.header("Accept", "application/json")
					.build();
			HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
			int status = response.statusCode();
			switch(status) {
				case 200:
					return addDownloadInfosTo(artifact, response.body());
				case 429:
					throw new RateLimitReached();
				default:
					logger.severe(String.format("What to do with status %s sent for url %s", status, url));
			}
			return artifact;
	}

	private ArtifactDetails addDownloadInfosTo(ArtifactDetails artifact, String text) throws IOException {
		Map content;
		try {
			content = FileHelper.getObjectMapper().readValue(text, Map.class);
			if(content.containsKey("data")) {
				Map data = (Map) content.get("data");
				if(data.containsKey("last_month")) {
					String downloadsText = data.get("last_month").toString();
					BigDecimal big = new BigDecimal(downloadsText);
					return ArtifactDetailsBuilder.toBuilder(artifact)
							.downloads(big.longValueExact())
							.build();
				}
			}
			return artifact;
		} catch (JacksonException e) {
			throw new IOException(e);
		}
	}

	/**
	 * This initially used https://pypi.org/stats url, which only gives the
	 * 100 most popular packages, which is wildlyu unsifficient.
	 * Fortunatly, https://github.com/hugovk/top-pypi-packages provides the
	 * top-8000 (which I trim to 1000) packages.
	 * @return
	 */
	public Collection<ArtifactDetails> loadPopularArtifacts() {
		String url = String.format(POPULAR);
		HttpRequest request = HttpRequest.newBuilder(URI.create(url))
				.header("Accept", "application/json")
				.build();
		try {
			HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
			if(response.statusCode()<300) {
				String text = response.body();
				Map content = FileHelper.getObjectMapper().readValue(text, Map.class);
				if(content.containsKey("rows")) {
					List<Map<String, String>> top = (List<Map<String, String>>) content.get("rows");
					return top.subList(0, 1000).stream()
							.filter(map -> map.containsKey("project"))
							.map(map -> map.get("project"))
							.map(name -> ArtifactDetailsBuilder.artifactDetails()
									.name(name)
									.build())
							.collect(Collectors.toList());
				} else {
					throw new PypiContentException("Content has changed, there is no more top_packages");
				}
			} else {
				throw new PypiHttpException("I don't handle http erros");
			}
		} catch (IOException | InterruptedException e) {
			throw new PypiExtractionException(e);
		}
	}
}
