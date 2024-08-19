package org.ndx.aadarchi.technology.detector.mvnrepository;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;

import org.ndx.aadarchi.technology.detector.helper.ArtifactLoader;
import org.ndx.aadarchi.technology.detector.model.ArtifactDetails;

/**
 * Load popular mvnrepository artifacts by browsing the popular pages
 */
public class PopularArtifactLoader implements ArtifactLoader<MvnContext> {

	private File cachedArtifacts;
	private String mvnRepositoryServer;

	PopularArtifactLoader(Path cache, String mvnRepositoryServer) {
		this.cachedArtifacts = new File(cache.toAbsolutePath().toFile(), "popular_artifacts.json");
		this.mvnRepositoryServer = mvnRepositoryServer;
	}

	@Override
	public Collection<ArtifactDetails> doLoadArtifacts(MvnContext context) throws Exception {
		return MvnArtifactLoaderHelper.loadPageList(context.newPage(), String.format("%s/popular", this.mvnRepositoryServer));
	}

	@Override
	public File getCachedArtifactsFile() {
		return cachedArtifacts;
	}
	
}