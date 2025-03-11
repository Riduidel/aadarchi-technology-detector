package org.ndx.aadarchi.technology.detector.indicators.github;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.camel.Exchange;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.ndx.aadarchi.technology.detector.indicators.TechnologyBased;
import org.ndx.aadarchi.technology.detector.model.Technology;

public interface GitHubBased extends TechnologyBased {
	public default boolean usesGitHubRepository(Exchange exchange) {
		return usesGitHubRepository(getTechnology(exchange));
	}

	public default boolean usesGitHubRepository(Technology technology) {
		return technology.repositoryUrl!=null && technology.repositoryUrl.contains("//github.com");
	}


	public default Optional<GHRepository> getGHReposigtory(Exchange exchange) throws IOException {
		return getGHRepository(getTechnology(exchange));
	}
	
	public default Optional<GHRepository> getGHRepository(Technology technology) throws IOException {
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
			path = pathElements.stream().collect(Collectors.joining("/"));
			return Optional.ofNullable(getGitHub().getRepository(path));
		} catch (IllegalArgumentException e) {
			return Optional.empty();
		} catch (MalformedURLException | URISyntaxException e) {
			return Optional.empty();
		}
	}

	public GitHub getGitHub();

}
