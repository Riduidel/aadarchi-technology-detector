package org.ndx.aadarchi.technology.detector.pypi;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.ndx.aadarchi.technology.detector.helper.TechEmpowerArtifactLoader;
import org.ndx.aadarchi.technology.detector.model.ArtifactDetails;
import org.ndx.aadarchi.technology.detector.model.ArtifactDetailsBuilder;

import com.github.fge.lambdas.Throwing;

public class PythonTechEmpowerArtifacts extends TechEmpowerArtifactLoader {

	public PythonTechEmpowerArtifacts(Path cache, Path techEmpowerFrameworks) {
		super(cache, techEmpowerFrameworks, "Python");
	}

	@Override
	protected Collection<ArtifactDetails> locateArtifactsIn(Stream<File> matchingFrameworksFolders) {
		return matchingFrameworksFolders
			.flatMap(framework -> Arrays.asList(
					new File(framework, "requirements.txt")
					).stream())
			.filter(File::exists)
			.map(Throwing.function(this::identifyDependenciesIn))
			.flatMap(Collection::stream)
			.collect(Collectors.toCollection(() -> new TreeSet<ArtifactDetails>()));
	}

	public Collection<ArtifactDetails> identifyDependenciesIn(File requirements) throws IOException {
		String text = FileUtils.readFileToString(requirements, "UTF-8");
		var returned = text.lines()
			.filter(line -> !line.isBlank())
			.filter(line -> !line.trim().startsWith("#"))
			.map(line -> line.split("[=>\\[<@]"))
			.map(symbols -> symbols[0])
			.filter(symbol -> !symbol.isBlank())
			.filter(symbol -> !symbol.contains("http://"))
			.filter(symbol -> !symbol.contains("https://"))
			.filter(symbol -> !symbol.startsWith("-e"))
			.map(dependencyName -> ArtifactDetailsBuilder.artifactDetails().name(dependencyName).build())
			.collect(Collectors.toList());
		return returned;
	}
}
