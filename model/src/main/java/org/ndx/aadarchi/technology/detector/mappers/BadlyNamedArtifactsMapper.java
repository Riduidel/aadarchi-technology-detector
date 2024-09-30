package org.ndx.aadarchi.technology.detector.mappers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.ndx.aadarchi.technology.detector.model.ArtifactDetails;

import io.github.emilyydev.asp.ProvidesService;

@ProvidesService(MappingGenerator.class)
public class BadlyNamedArtifactsMapper implements MappingGenerator {
	private static final Logger logger = Logger.getLogger(BadlyNamedArtifactsMapper.class.getName());

	@Override
	public void generateMapping(Collection<ArtifactDetails> artifactDetails, Path resources) {
		Properties mappings = artifactDetails.stream()
			.filter(artifact -> artifact.getCoordinates()!=null)
			.map(artifact ->
				Map.entry(
					String.format("%s.%s", artifact.getGroupId(), artifact.getArtifactId()), 
					String.format("%s:%s", artifact.getGroupId(), artifact.getArtifactId())
				)
			)
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, () -> new Properties()));
		File output = new File(resources.toFile(), ArtifactDetails.BADLY_NAMED_ARTIFACTS_MAPPING);
		writeProperties(output, mappings);
	}
}
