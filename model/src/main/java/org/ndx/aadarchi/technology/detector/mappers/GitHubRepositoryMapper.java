package org.ndx.aadarchi.technology.detector.mappers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.stream.Collectors;

import org.ndx.aadarchi.technology.detector.augmenters.github.GitHubProjects;
import org.ndx.aadarchi.technology.detector.model.ArtifactDetails;

import io.github.emilyydev.asp.ProvidesService;

@ProvidesService(MappingGenerator.class)
public class GitHubRepositoryMapper implements MappingGenerator {

	@Override
	public void generateMapping(Collection<ArtifactDetails> artifactDetails, Path resources) {
		File output = new File(resources.toFile(), GitHubProjects.GITHUB_REPOSITORIES);
		Properties input = withPropertiesLoadedFrom(output);
		Properties mappings = artifactDetails.stream()
				.filter(artifact -> artifact.getUrls()!=null)
				.filter(artifact -> artifact.getUrls().containsKey("github.com"))
				.filter(artifact -> !artifactInInput(input, artifact))
				.map(artifact -> Map.entry(
						artifact.getIdentifier(),
						artifact.getUrls().get("github.com")))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, () -> new Properties()));
		mappings.putAll(input);
		writeProperties(output, mappings);
		
	}

	private boolean artifactInInput(Properties input, ArtifactDetails artifact) {
		if(input.containsKey(artifact.getCoordinates()))
			return input.getProperty(artifact.getCoordinates()).equals(artifact.getUrls().get("github.com"));
		else if(input.containsKey(artifact.getGroupId()))
			return input.getProperty(artifact.getGroupId()).equals(artifact.getUrls().get("github.com"));
		else
			return false;
	}
}
