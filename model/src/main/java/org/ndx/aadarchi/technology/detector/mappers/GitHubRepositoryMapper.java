package org.ndx.aadarchi.technology.detector.mappers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import org.ndx.aadarchi.technology.detector.augmenters.github.AddGitHubStarsAtPeriod;
import org.ndx.aadarchi.technology.detector.model.ArtifactDetails;

import io.github.emilyydev.asp.ProvidesService;

@ProvidesService(MappingGenerator.class)
public class GitHubRepositoryMapper implements MappingGenerator {

	@Override
	public void generateMapping(Collection<ArtifactDetails> artifactDetails, Path resources) {
		Properties mappings = artifactDetails.stream()
				.filter(artifact -> artifact.getUrls()!=null)
				.filter(artifact -> artifact.getUrls().containsKey("github.com"))
				.map(artifact -> Map.entry(
						artifact.getIdentifier(),
						artifact.getUrls().get("github.com")))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, () -> new Properties()));
		File output = new File(resources.toFile(), AddGitHubStarsAtPeriod.GITHUB_REPOSITORIES);
		writeProperties(output, mappings);
	}
}
