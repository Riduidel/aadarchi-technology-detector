package org.ndx.aadarchi.technology.detector.npmjs;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.ndx.aadarchi.technology.detector.helper.FileHelper;
import org.ndx.aadarchi.technology.detector.loader.BasicArtifactLoader;
import org.ndx.aadarchi.technology.detector.model.ArtifactDetails;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.github.fge.lambdas.Throwing;

public class PopularNpmArtifacts extends BasicArtifactLoader<NpmjsContext> {
	public static final List<String> POPULAR_ARTIFACTS_PAGES = 
			IntStream.of(0, 251, 501, 751)
			.mapToObj(index -> String.format("https://registry.npmjs.com/-/v1/search?text=not:unstable&popularity=1.0&from=%d&size=250", index))
			.collect(Collectors.toList())
			;
	
	public static class PackageResponse {
		@JsonProperty("package")
		public ArtifactDetails artifact;
	}
	
	public static class NpmJsResponse {
		public List<PopularNpmArtifacts.PackageResponse> objects;
		public long total;
		public String time;
	}

	private HttpClient client;
	
	PopularNpmArtifacts(Path cache, HttpClient client) {
		this.client = client;
		registerCachedArtifacts(cache, "popular_npm_artifacts.json");
	}

	@Override
	public Collection<ArtifactDetails> doLoadArtifacts(NpmjsContext context) throws Exception {
		return POPULAR_ARTIFACTS_PAGES.stream()
			.map(Throwing.function(this::doLoadArtifactsFrom))
			.flatMap(List::stream)
			.collect(Collectors.toList());
	}
	
	List<ArtifactDetails> doLoadArtifactsFrom(String url) throws IOException, InterruptedException {
		HttpRequest request = HttpRequest.newBuilder(URI.create(url)).build();
		HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
		PopularNpmArtifacts.NpmJsResponse jsonResponse = FileHelper.getObjectMapper().readValue(response.body(), new TypeReference<PopularNpmArtifacts.NpmJsResponse>() {});
		List<ArtifactDetails> returned = jsonResponse.objects.stream()
				.map(a -> a.artifact)
				.collect(Collectors.toList());
        return returned;

	}
}