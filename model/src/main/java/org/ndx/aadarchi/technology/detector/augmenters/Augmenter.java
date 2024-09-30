package org.ndx.aadarchi.technology.detector.augmenters;

import org.ndx.aadarchi.technology.detector.loader.ExtractionContext;
import org.ndx.aadarchi.technology.detector.model.ArtifactDetails;

/**
 * An augmenter has the capability, given a context, to add informations to an
 * {@link ArtifactDetails} object.
 */
public interface Augmenter {
	/**
	 * Configure services order. It allows easy dependency building.
	 * @return 100 as default.
	 */
	public default int order() {
		return 1000;
	}
	
	public ArtifactDetails augment(ExtractionContext context, ArtifactDetails source);
}
