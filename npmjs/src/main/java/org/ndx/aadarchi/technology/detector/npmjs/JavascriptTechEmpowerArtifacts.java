package org.ndx.aadarchi.technology.detector.npmjs;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.ndx.aadarchi.technology.detector.helper.FileHelper;
import org.ndx.aadarchi.technology.detector.loader.TechEmpowerArtifactLoader;
import org.ndx.aadarchi.technology.detector.model.ArtifactDetails;
import org.ndx.aadarchi.technology.detector.model.ArtifactDetailsBuilder;

public class JavascriptTechEmpowerArtifacts extends TechEmpowerArtifactLoader<NpmjsContext> {

	public JavascriptTechEmpowerArtifacts(Path cache, Path techEmpowerFrameworks) {
		super(cache, techEmpowerFrameworks, "JavaScript");
	}

	@Override
	protected Collection<ArtifactDetails> locateArtifactsIn(Stream<File> matchingFrameworksFolders) {
		return matchingFrameworksFolders
				.flatMap(framework -> Arrays.asList(
						new File(framework, "package.json")
						).stream())
				.filter(File::exists)
				.map(this::toArtifactDetails)
				.flatMap(Collection::stream)
				.collect(Collectors.toCollection(() -> new TreeSet<ArtifactDetails>()));
	}
	protected Collection<ArtifactDetails> toArtifactDetails(File packageJson) {
		try {
			Collection<ArtifactDetails> returned = new ArrayList<ArtifactDetails>();
			String packageJsonContent = FileUtils.readFileToString(packageJson, "UTF-8");
			Map content = FileHelper.getObjectMapper().readValue(packageJsonContent, Map.class);
			if(content.containsKey("dependencies")) {
				@SuppressWarnings("unchecked")
				Map<String, ?> dependencies = (Map<String, ?>) content.get("dependencies");
				returned.addAll(dependencies.keySet().stream()
					.map(text -> ArtifactDetailsBuilder.artifactDetails().name(text).build())
					.collect(Collectors.toList()));
			}
			return returned;
		} catch(IOException e) {
			throw new RuntimeException(String.format("Can't read package.json file %s", packageJson), e);
		}
	}
}
