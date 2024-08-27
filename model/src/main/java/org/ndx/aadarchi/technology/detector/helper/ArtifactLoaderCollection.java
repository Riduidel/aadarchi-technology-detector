package org.ndx.aadarchi.technology.detector.helper;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.ndx.aadarchi.technology.detector.model.ArtifactDetails;

import com.github.fge.lambdas.Throwing;

public class ArtifactLoaderCollection<Context extends ExtractionContext> extends BasicArtifactLoader<Context> {

	private List<ArtifactLoader<Context>> artifactLoaders;

	public ArtifactLoaderCollection(Path cache, 
			ArtifactLoader<Context>...artifactLoaders) {
		registerCachedArtifacts(cache, "all_artifacts.json");
		this.artifactLoaders = Arrays.asList(artifactLoaders);
	}

	public ArtifactLoaderCollection(Path cache, Collection<ArtifactLoader<Context>> artifactLoaderCollection) {
		registerCachedArtifacts(cache, "all_artifacts.json");
		this.artifactLoaders = new ArrayList<>(artifactLoaderCollection);
	}

	@Override
	public Collection<ArtifactDetails> doLoadArtifacts(Context context) throws Exception {
		return artifactLoaders.stream()
				.flatMap(Throwing.function(loader -> loader.loadArtifacts(context).stream()))
				.collect(Collectors.toCollection(() -> new TreeSet<ArtifactDetails>()));
	}

}
