package org.ndx.aadarchi.technology.detector.augmenters;

import org.ndx.aadarchi.technology.detector.loader.ExtractionContext;
import org.ndx.aadarchi.technology.detector.model.ArtifactDetails;
import org.ndx.aadarchi.technology.detector.model.ArtifactDetailsBuilder;

public class AddGitHubStarsAtPeriod implements Augmenter {

	@Override
	public ArtifactDetails augment(ExtractionContext context, ArtifactDetails source) {
		if(source.getUrls().containsKey("github.com")) {
			return doAugment(context, source);
		}
		return source;
	}

	private ArtifactDetails doAugment(ExtractionContext context, ArtifactDetails source) {
		String repository = source.getUrls().get("github.com");
		ArtifactDetailsBuilder builder = ArtifactDetailsBuilder.toBuilder(source);
		
		return builder.build();
	}

}
