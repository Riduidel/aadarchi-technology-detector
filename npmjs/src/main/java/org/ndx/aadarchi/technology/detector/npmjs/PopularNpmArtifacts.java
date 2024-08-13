package org.ndx.aadarchi.technology.detector.npmjs;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.ndx.aadarchi.technology.detector.helper.InterestingArtifactsDetailsDownloader;
import org.ndx.aadarchi.technology.detector.helper.NoContext;
import org.ndx.aadarchi.technology.detector.model.ArtifactDetails;

import com.github.fge.lambdas.Throwing;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

public class PopularNpmArtifacts implements ArtifactLoader {
	public static final List<String> POPULAR_ARTIFACTS_PAGES = 
			IntStream.of(0, 251, 501, 751)
			.mapToObj(index -> String.format("https://registry.npmjs.com/-/v1/search?text=not:unstable&popularity=1.0&from=%d&size=250", index))
			.collect(Collectors.toList())
			;
	
	public static class PackageResponse {
		@SerializedName("package")
		public ArtifactDetails artifact;
	}
	
	public static class NpmJsResponse {
		public List<PopularNpmArtifacts.PackageResponse> objects;
		public long total;
		public String time;
	}

	private HttpClient client;
	
	private final File cachedArtifacts;
	
	PopularNpmArtifacts(Path cache, HttpClient client) {
		this.client = client;
		cachedArtifacts = new File(cache.toFile(), "popularNpmArtifacts.json");
	}

	@Override
	public List<ArtifactDetails> doLoadArtifacts() throws IOException, InterruptedException {
		return POPULAR_ARTIFACTS_PAGES.stream()
			.map(Throwing.function(this::doLoadArtifactsFrom))
			.flatMap(List::stream)
			.collect(Collectors.toList());
	}
	
	List<ArtifactDetails> doLoadArtifactsFrom(String url) throws IOException, InterruptedException {
		HttpRequest request = HttpRequest.newBuilder(URI.create(url)).build();
		HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
		PopularNpmArtifacts.NpmJsResponse jsonResponse = InterestingArtifactsDetailsDownloader.gson.fromJson(response.body(), new TypeToken<PopularNpmArtifacts.NpmJsResponse>() {});
		List<ArtifactDetails> returned = jsonResponse.objects.stream()
				.map(a -> a.artifact)
				.collect(Collectors.toList());
        return returned;

	}

	@Override
	public File getCachedArtifactsFile() {
		return cachedArtifacts;
	}
	
}