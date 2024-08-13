package org.ndx.aadarchi.technology.detector.mvnrepository;

import org.apache.maven.artifact.Artifact;
import org.ndx.aadarchi.technology.detector.model.ArtifactDetails;

public class ArtifactDetailsUtils {

	public static String getArtifactUrl(ArtifactDetails details, String mvnRepositoryServer) {
		Artifact split = details.toArtifact();
		return String.format("%s/artifact/%s/%s", mvnRepositoryServer, 
				split.getGroupId(),
				split.getArtifactId());
	}

}
