package org.ndx.aadarchi.technology.detector.mappers;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
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
		for(Function<ArtifactDetails, String> extractor : ArtifactDetails.GITHUB_REPO_EXTRACTORS) {
			String key = extractor.apply(artifact);
			if(key!=null) {
				if(input.containsKey(key)) {
					if(input.getProperty(key).equals(artifact.getUrls().get("github.com"))) {
						return true;
					}
				}
			}
		}
		return false;
	}
}
