package org.ndx.aadarchi.technology.detector.mvnrepository;

import java.nio.file.Path;
import java.util.Collection;

import org.ndx.aadarchi.technology.detector.helper.BasicArtifactLoader;
import org.ndx.aadarchi.technology.detector.model.ArtifactDetails;

/**
 * Load popular mvnrepository artifacts by browsing the popular pages
 */
public class PopularArtifactLoader extends BasicArtifactLoader<MvnContext> {

	private String mvnRepositoryServer;

	PopularArtifactLoader(Path cache, String mvnRepositoryServer) {
		registerCachedArtifacts(cache, "popular_artifacts.json");
		this.mvnRepositoryServer = mvnRepositoryServer;
	}

	@Override
	public Collection<ArtifactDetails> doLoadArtifacts(MvnContext context) throws Exception {
		return MvnArtifactLoaderHelper.loadPageList(context.newPage(), String.format("%s/popular", this.mvnRepositoryServer));
	}
}