package org.ndx.aadarchi.technology.detector.augmenters.stackoverflow;

import java.time.LocalDate;

import org.ndx.aadarchi.technology.detector.augmenters.Augmenter;
import org.ndx.aadarchi.technology.detector.loader.ExtractionContext;
import org.ndx.aadarchi.technology.detector.model.ArtifactDetails;

import io.github.emilyydev.asp.ProvidesService;

/**
 * This augmenter is only here to have a stackoverflow tag linked to an artifact.
 * I hope to find a way to not resort to using LLM.
 */
@ProvidesService(Augmenter.class)
public class StackOverflowTagLoader implements Augmenter {

	@Override
	public ArtifactDetails augment(ExtractionContext context, ArtifactDetails source, LocalDate date) {
		return source;
	}

}
