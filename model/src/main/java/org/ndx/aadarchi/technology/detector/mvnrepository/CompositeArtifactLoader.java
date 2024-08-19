package org.ndx.aadarchi.technology.detector.mvnrepository;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.ndx.aadarchi.technology.detector.helper.ArtifactLoader;
import org.ndx.aadarchi.technology.detector.helper.ExtractionContext;
import org.ndx.aadarchi.technology.detector.model.ArtifactDetails;

import com.github.fge.lambdas.Throwing;

public class CompositeArtifactLoader<Context extends ExtractionContext> implements ArtifactLoader<Context> {

	private List<ArtifactLoader<Context>> artifactLoaders;
	private File cachedArtifacts;

	public CompositeArtifactLoader(Path cache, 
			ArtifactLoader<Context>...artifactLoaders) {
		this.cachedArtifacts = new File(cache.toAbsolutePath().toFile(), "all_artifacts.json");
		this.artifactLoaders = Arrays.asList(artifactLoaders);
	}

	@Override
	public Collection<ArtifactDetails> doLoadArtifacts(Context context) throws Exception {
		return artifactLoaders.stream()
				.flatMap(Throwing.function(loader -> loader.loadArtifacts(context).stream()))
				.collect(Collectors.toCollection(() -> new TreeSet<ArtifactDetails>()));
	}

	@Override
	public File getCachedArtifactsFile() {
		return cachedArtifacts;
	}

}
