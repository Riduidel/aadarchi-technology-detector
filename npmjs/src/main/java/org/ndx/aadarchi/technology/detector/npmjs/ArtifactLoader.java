package org.ndx.aadarchi.technology.detector.npmjs;

import java.io.File;
import java.util.List;

import org.ndx.aadarchi.technology.detector.helper.FileHelper;
import org.ndx.aadarchi.technology.detector.model.ArtifactDetails;

public interface ArtifactLoader {

	public default List<ArtifactDetails> loadArtifacts() throws Exception {
		File cachedArtifacts = getCachedArtifactsFile();
		if(cachedArtifacts.lastModified()<System.currentTimeMillis()-(1000*60*60*24)) {
			cachedArtifacts.delete();
		}
		if(!cachedArtifacts.exists()) {
			FileHelper.writeToFile(doLoadArtifacts(), cachedArtifacts);
		}
		return FileHelper.readFromFile(cachedArtifacts);
	}

	public List<ArtifactDetails> doLoadArtifacts() throws Exception;

	public File getCachedArtifactsFile();
	
}