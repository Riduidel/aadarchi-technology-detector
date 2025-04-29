package org.ndx.aadarchi.technology.detector.indicators.github;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.apache.camel.Exchange;
import org.apache.camel.util.Pair;
import org.ndx.aadarchi.technology.detector.indicators.TechnologyBased;
import org.ndx.aadarchi.technology.detector.model.Technology;

public interface GitHubBased extends TechnologyBased {
	default boolean usesGitHubRepository(Exchange exchange) {
		return usesGitHubRepository(getTechnology(exchange));
	}

	default boolean usesGitHubRepository(Technology technology) {
		return technology.repositoryUrl!=null && technology.repositoryUrl.contains("//github.com");
	}


	default Optional<Pair<String>> getRepository(Exchange exchange) throws IOException {
		return getRepository(getTechnology(exchange));
	}
	
	default Optional<Pair<String>> getRepository(Technology technology) throws IOException {
		String fullRepositoryUrl = technology.repositoryUrl;
		URL url;
		try {
			url = new URI(fullRepositoryUrl).toURL();
			String path = url.getPath();
			if(path.startsWith("/")) {
				path = path.substring(1);
			}
			List<String> pathElements = Arrays.asList(path.split("/"));
			if(pathElements.size()>2) {
				pathElements = pathElements.subList(0, 2);
			}
			return Optional.of(new Pair<>(pathElements.get(0), pathElements.get(1)));
		} catch (IllegalArgumentException | MalformedURLException | URISyntaxException e) {
			return Optional.empty();
		}
    }
}
