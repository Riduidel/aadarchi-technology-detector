package org.ndx.aadarchi.technology.detector.augmenters;

import java.time.LocalDate;

import org.ndx.aadarchi.technology.detector.loader.ExtractionContext;
import org.ndx.aadarchi.technology.detector.model.ArtifactDetails;
import org.ndx.aadarchi.technology.detector.model.ArtifactDetailsBuilder;

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

	/**
	 * Augment the given artifact at a given date
	 * @param context augmentation context (contains utilities)
	 * @param source artifact to augment
	 * @param date date at which we want to have our artifact augmented
	 * @return an updated artifact, typically created with {@link ArtifactDetailsBuilder#toBuilder(ArtifactDetails)}
	 */
	public ArtifactDetails augment(ExtractionContext context, ArtifactDetails source, LocalDate date);
}
