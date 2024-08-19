package org.ndx.aadarchi.technology.detector.helper;

import java.io.File;
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
		return cachedArtifacts.lastModified()<System.currentTimeMillis()-(1000*60*60*24);
	}

	public Collection<ArtifactDetails> doLoadArtifacts(Context context) throws Exception;

	public File getCachedArtifactsFile();
	
}