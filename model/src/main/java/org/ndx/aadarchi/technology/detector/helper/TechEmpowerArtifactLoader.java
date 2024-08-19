package org.ndx.aadarchi.technology.detector.helper;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.ndx.aadarchi.technology.detector.model.ArtifactDetails;

public abstract class TechEmpowerArtifactLoader implements ArtifactLoader<ExtractionContext> {
	public static final Logger logger = Logger.getLogger(TechEmpowerArtifactLoader.class.getName());

	private Path techEmpowerFrameworks;
	private final File cachedArtifacts;
	protected final String languageFolder;
	
	public TechEmpowerArtifactLoader(Path cache, Path techEmpowerFrameworks, String languageFolder) {
		this.techEmpowerFrameworks = techEmpowerFrameworks.toAbsolutePath();
		this.languageFolder = languageFolder;
		cachedArtifacts = new File(cache.toAbsolutePath().toFile(), "techempower_artifacts.json");
	}

	@Override
	public Collection<ArtifactDetails> doLoadArtifacts(ExtractionContext context) throws Exception {
		return locateArtifactsIn(findMatchingFrameworksFolders(techEmpowerFrameworks));
	}


	protected abstract Collection<ArtifactDetails> locateArtifactsIn(Stream<File> matchingFrameworksFolders);

	@Override
	public File getCachedArtifactsFile() {
		return cachedArtifacts;
	}

	private Stream<File> findMatchingFrameworksFolders(Path techEmpowerFrameworks) {
		if(techEmpowerFrameworks.toFile().exists()) {
			File[] children = techEmpowerFrameworks.toFile().listFiles();
			if(children.length==0) {
				logger.warning(
						String.format("There are no children in %s", techEmpowerFrameworks.toString()));
			} else {
				return Stream.of(children)
					.filter(folder -> folder.getName().equals(languageFolder))
					.flatMap(javaFolder -> Stream.of(javaFolder.listFiles()))
					.filter(File::isDirectory);
			}
		} else {
			logger.warning(
					String.format("%s doesn't exists. Is it really cloned?", techEmpowerFrameworks.toString()));
		}
		return new ArrayList<File>().stream();
	}
}