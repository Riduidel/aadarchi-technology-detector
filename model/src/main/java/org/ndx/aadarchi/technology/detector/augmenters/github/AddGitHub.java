package org.ndx.aadarchi.technology.detector.augmenters.github;

import java.time.LocalDate;
import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

import org.ndx.aadarchi.technology.detector.augmenters.Augmenter;
import org.ndx.aadarchi.technology.detector.loader.ExtractionContext;
import org.ndx.aadarchi.technology.detector.model.ArtifactDetails;
import org.ndx.aadarchi.technology.detector.model.ArtifactDetailsBuilder;
import org.ndx.aadarchi.technology.detector.model.GitHubDetails;
import org.ndx.aadarchi.technology.detector.model.GitHubDetailsBuilder;

import com.fasterxml.jackson.databind.JsonNode;

import io.github.emilyydev.asp.ProvidesService;

@ProvidesService(Augmenter.class)
public class AddGitHub implements Augmenter {
	private static final Logger logger = Logger.getLogger(AddGitHub.class.getName());
	private final Set<String> alreadyLoggedProjects = new TreeSet<>();
	public static final int ADD_GITHUB_OBJECT = 100;

	@Override
	public int order() {
		return ADD_GITHUB_OBJECT;
	}

	@Override
	public ArtifactDetails augment(ExtractionContext context, ArtifactDetails source, LocalDate date) {
		if(source.getGithubDetails()==null) {
			if(GitHubProjects.contains(source)) {
				return doAugment(context, source,GitHubProjects.getGitHubPath(source), date);
			} else if(source.getUrls()!=null && source.getUrls().containsKey("github.com")) {
				return doAugment(context, source, GitHubProjects.getGitHubPath(source.getUrls().get("github.com")), date);
			} else {
				if(!alreadyLoggedProjects.contains(source.getIdentifier())) {
					logger.warning(String.format("There doesn't seems to be any github repo for "+source.getIdentifier()));
					alreadyLoggedProjects.add(source.getIdentifier());
				}
			}
		}
		return source;
	}

	private ArtifactDetails doAugment(ExtractionContext context, ArtifactDetails source, String path, LocalDate date) {
		GitHubGraphQLClient helper = GitHubGraphQLClient.getClient(context.getGithubToken());
		JsonNode repositoryId = helper.runGraphQLQuery(GitHubGraphQLClient.REPOSITORY_ID, GitHubDetails.getOwner(path), GitHubDetails.getRepository(path));
		if(repositoryId.has("errors")) {
			logger.warning("Impossible to add GitHub repository to "+source.getName()+"\ndue to "+repositoryId);
			return source;
		} else {
			ArtifactDetailsBuilder builder = ArtifactDetailsBuilder.toBuilder(source);
			return builder.githubDetails(GitHubDetailsBuilder.gitHubDetails()
						.stargazers(Optional.empty())
						.path(path)
						.build())
					.build();
		}
	}

}
