package org.ndx.aadarchi.technology.detector.pypi;

import java.nio.file.Path;
import java.util.Collection;
import java.util.stream.Collectors;

import org.ndx.aadarchi.technology.detector.helper.ArtifactLoader;
import org.ndx.aadarchi.technology.detector.helper.ArtifactLoaderCollection;
import org.ndx.aadarchi.technology.detector.model.ArtifactDetails;

import com.github.fge.lambdas.Throwing;

public class DetailFetchingArtifactLoaderCollection extends ArtifactLoaderCollection<PypiContext> {

	public DetailFetchingArtifactLoaderCollection(Path cache,
			Collection<ArtifactLoader<? super PypiContext>> artifactLoaderCollection) {
		super(cache, artifactLoaderCollection);
	}

	@Override
	public Collection<ArtifactDetails> doLoadArtifacts(PypiContext context) throws Exception {
		Collection<ArtifactDetails> withOnlyNames = super.doLoadArtifacts(context);
		return withOnlyNames
			.stream()
			.map(Throwing.function(context::addDetails))
			.collect(Collectors.toList());
	}
}
