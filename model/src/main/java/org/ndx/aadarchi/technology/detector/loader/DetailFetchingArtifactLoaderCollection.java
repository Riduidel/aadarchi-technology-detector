package org.ndx.aadarchi.technology.detector.loader;

import java.nio.file.Path;
import java.util.Collection;
import java.util.stream.Collectors;

import org.ndx.aadarchi.technology.detector.model.ArtifactDetails;

import com.github.fge.lambdas.Throwing;

/**
 * Common class for details fetchnig artifact loader.
 * Given artifacts identifiers, this artifact loader is able to add details to them thanks to the DetailsAddingContext interface.
 */
public class DetailFetchingArtifactLoaderCollection<Type extends DetailsFetchingContext> extends ArtifactLoaderCollection<Type> {

	public DetailFetchingArtifactLoaderCollection(Path cache,
			Collection<ArtifactLoader<Type>> artifactLoaderCollection) {
		super(cache, artifactLoaderCollection);
	}

	@Override
	public Collection<ArtifactDetails> doLoadArtifacts(Type context) throws Exception {
		Collection<ArtifactDetails> withOnlyNames = super.doLoadArtifacts(context);
		return withOnlyNames
			.stream()
			.map(Throwing.function(context::addDetails))
			.collect(Collectors.toList());
	}
}
