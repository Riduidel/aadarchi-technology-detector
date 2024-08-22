package org.ndx.aadarchi.technology.detector.helper;

import java.io.File;
import java.time.Duration;
import java.util.Collection;

import org.ndx.aadarchi.technology.detector.model.ArtifactDetails;

public interface ArtifactLoader<Context extends ExtractionContext> {

	public default Collection<ArtifactDetails> loadArtifacts(Context context) throws Exception {
		File cachedArtifacts = getCachedArtifactsFile();
		if(isCachedArtifactsObsolete(cachedArtifacts)) {
			cachedArtifacts.delete();
		}
		if(!cachedArtifacts.exists()) {
			FileHelper.writeToFile(doLoadArtifacts(context), cachedArtifacts);
		}
		return FileHelper.readFromFile(cachedArtifacts);
	}

	public default boolean isCachedArtifactsObsolete(File cachedArtifacts) {
		return cachedArtifacts.lastModified()<System.currentTimeMillis()-getCacheDelayInSeconds()*1000;
	}

	/**
	 * The caching delay for the result of this artifact loader
	 * @return default value is one day
	 */
	public default long getCacheDelayInSeconds() {
		return getCacheDelay().toSeconds();
	}

	public default Duration getCacheDelay() {
		return Duration.ofDays(1);
	}

	public Collection<ArtifactDetails> doLoadArtifacts(Context context) throws Exception;

	public File getCachedArtifactsFile();
	
}