package org.ndx.aadarchi.technology.detector.loader;

import org.ndx.aadarchi.technology.detector.model.ArtifactDetails;

public interface DetailsFetchingContext extends ExtractionContext {
	public ArtifactDetails addDetails(ArtifactDetails source) throws Exception;
}
