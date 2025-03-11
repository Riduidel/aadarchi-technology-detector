package org.ndx.aadarchi.technology.detector.indicators.github;

import java.io.IOException;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.kohsuke.github.GitHub;

import jakarta.enterprise.inject.Produces;

public class GitHubProducer {
	
	@Produces public GitHub createGitHub(@ConfigProperty(name = "tech-trends.github.token") String githubToken) throws IOException {
		return GitHub.connectUsingOAuth(githubToken);
	}
}
