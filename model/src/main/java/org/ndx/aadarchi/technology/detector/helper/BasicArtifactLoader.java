package org.ndx.aadarchi.technology.detector.helper;

import java.io.File;
import java.nio.file.Path;

public abstract class BasicArtifactLoader<Context extends ExtractionContext> implements ArtifactLoader<Context> {
	private File cachedArtifactsFile;

	public void registerCachedArtifacts(Path cache, String filename) {
		cachedArtifactsFile = new File(cache.toAbsolutePath().toFile(), filename);
	}

	@Override
	public File getCachedArtifactsFile() {
		return cachedArtifactsFile;
	}
}
