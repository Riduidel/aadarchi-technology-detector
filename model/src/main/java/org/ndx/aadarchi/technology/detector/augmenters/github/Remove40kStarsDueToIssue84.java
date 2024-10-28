package org.ndx.aadarchi.technology.detector.augmenters.github;

import java.time.LocalDate;
import java.util.Optional;

import org.ndx.aadarchi.technology.detector.augmenters.Augmenter;
import org.ndx.aadarchi.technology.detector.loader.ExtractionContext;
import org.ndx.aadarchi.technology.detector.model.ArtifactDetails;
import org.ndx.aadarchi.technology.detector.model.ArtifactDetailsBuilder;
import org.ndx.aadarchi.technology.detector.model.GitHubDetails;
import org.ndx.aadarchi.technology.detector.model.GitHubDetailsBuilder;

import io.github.emilyydev.asp.ProvidesService;

@ProvidesService(Augmenter.class)
public class Remove40kStarsDueToIssue84 implements Augmenter {
	@Override
	public int order() {
		return AddGitHub.ADD_GITHUB_OBJECT+1;
	}

	@Override
	public ArtifactDetails augment(ExtractionContext context, ArtifactDetails source, LocalDate date) {
		GitHubDetails githubDetails = source.getGithubDetails();
		if(githubDetails!=null) {
			if(githubDetails.getStargazers().isPresent()) {
				if(githubDetails.getStargazers().get()==40_000) {
					return ArtifactDetailsBuilder.toBuilder(source)
							.githubDetails(GitHubDetailsBuilder.toBuilder(githubDetails)
									.stargazers(Optional.empty())
									.build())
							.build();
				}
			}
		}
		return source;
	}

}
