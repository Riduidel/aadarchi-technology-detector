package org.ndx.aadarchi.technology.detector.augmenters.github;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Collection;
import java.util.Optional;
import java.util.TreeSet;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.ndx.aadarchi.technology.detector.helper.FileHelper;
import org.ndx.aadarchi.technology.detector.helper.InterestingArtifactsDetailsDownloader;
import org.ndx.aadarchi.technology.detector.model.GitHubDetails;

import com.fasterxml.jackson.databind.JsonNode;

import tech.habegger.assertj.jackson.JsonNodeAssertions;

class AddGitHubStarsAtPeriodTest extends AddGitHubStarsAtPeriod {
	static String TESTED_GITHUB_REPO = "Riduidel/aadarchi-technology-detector";
	private static String token;
	private static GitHub github;

	@BeforeAll
	public static void fetchGitHubToken() throws IOException {
		token = System.getProperty("aadarchi.github.token");
		github = new GitHubBuilder()
				.withJwtToken(token)
				.build();
	}
	
	@Test void can_fetch_login() throws IOException, InterruptedException {
		// Given
		String requestText = "{query { viewer { login }}}";
		HttpRequest request = GitHubGraphQLClient.getClient(token).createGraphQLQuery(token, requestText);
		// When
		HttpResponse<String> response = InterestingArtifactsDetailsDownloader.client.send(request, BodyHandlers.ofString());
		// Then
		Assertions.assertThat(response.statusCode()).isLessThan(300);
		JsonNode node = FileHelper.getObjectMapper().readTree(response.body());
		JsonNodeAssertions.assertThat(node).asString("/errors").isEqualTo("");
		JsonNodeAssertions.assertThat(node).extracting("/data/login").isNotEqualTo("");
	}

	@Test
	void can_fetch_stargazers_count() throws IOException {
		// Given
		AddGitHubStarsAtPeriod tested = new AddGitHubStarsAtPeriod();
		GitHubDetails details = new GitHubDetails(TESTED_GITHUB_REPO, Optional.empty());
		GHRepository repository = github.getRepository(TESTED_GITHUB_REPO);
		int expected = repository.getStargazersCount();
		// Just to be sure we can get it
		Assertions.assertThat(expected).isGreaterThan(1);
		// When
		Integer actual = tested.getCurrentStargazersCount(token, details);
		// Then
		Assertions.assertThat(actual)
			.isEqualTo(expected);
	}

	@Test
	void can_fetch_stargazers_list() throws IOException {
		// Given
		AddGitHubStarsAtPeriod tested = new AddGitHubStarsAtPeriod();
		GitHubDetails details = new GitHubDetails(TESTED_GITHUB_REPO, Optional.empty());
		GHRepository repository = github.getRepository(TESTED_GITHUB_REPO);
		int stargazersCount = repository.getStargazersCount();
		// Just to be sure we can get it
		Assertions.assertThat(stargazersCount).isGreaterThan(1);
		// When
		Collection<Stargazer> allStargazers = tested.doGetAllStargazers(token, details, stargazersCount, new TreeSet<Stargazer>());
		// Then
		Assertions.assertThat(allStargazers).hasSize(stargazersCount);
	}

}
