package org.ndx.aadarchi.technology.detector.packagist;

import com.fasterxml.jackson.core.type.TypeReference;
import org.ndx.aadarchi.technology.detector.helper.FileHelper;
import org.ndx.aadarchi.technology.detector.loader.BasicArtifactLoader;
import org.ndx.aadarchi.technology.detector.model.ArtifactDetails;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PopularPackagistArtifacts extends BasicArtifactLoader<PackagistContext> {
    public static final List<String> POPULAR_ARTIFACTS_PAGES =
            IntStream.of(0, 1, 2, 3, 4, 5)
                    .mapToObj(index -> String.format("https://packagist.org/explore/popular.json?per_page=100&page=%d", index))
                    .collect(Collectors.toList());
    private HttpClient client;

    public PopularPackagistArtifacts(Path cache, HttpClient client) {
        this.client = client;
        registerCachedArtifacts(cache, "popular_packagist_artifacts.json");
    }

    public static class PackagistResponse {
        public List<ArtifactDetails> packages;
        public long total;
        public String next;
    }

    @Override
    public Collection<ArtifactDetails> doLoadArtifacts(PackagistContext context) throws Exception {
        return POPULAR_ARTIFACTS_PAGES.stream()
                .map(this::fetchArtifactsFrom)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    private List<ArtifactDetails> fetchArtifactsFrom(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url)).build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            PopularPackagistArtifacts.PackagistResponse jsonResponse = FileHelper.getObjectMapper().readValue(response.body(), new TypeReference<PackagistResponse>() {});
            return new ArrayList<>(jsonResponse.packages);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Error fetching Packagist popular artifacts", e);
        }
    }

}
