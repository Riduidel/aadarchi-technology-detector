package org.ndx.aadarchi.technology.detector.augmenters;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.ndx.aadarchi.technology.detector.loader.ExtractionContext;
import org.ndx.aadarchi.technology.detector.model.ArtifactDetails;

public class Augmenters {
	 private static ServiceLoader<Augmenter> augmentersLoader
     	= ServiceLoader.load(Augmenter.class);
	 private static Optional<List<Augmenter>> loaded = Optional.empty();
	 
	 public static List<Augmenter> getAugmenters() {
		 if(loaded.isEmpty())
		 	loaded = Optional.of(StreamSupport.stream(augmentersLoader.spliterator(), false)
				 .sorted(Comparator.comparing(a -> ((Augmenter) a).order())
						 .thenComparing(a -> a.getClass().getName()))
				 .collect(Collectors.toList()));
		 return loaded.get();
	 }

	 public static <Context extends ExtractionContext> Collection<ArtifactDetails> augmentArtifacts(Context context,
			Collection<ArtifactDetails> artifacts) {
		List<ArtifactDetails> augmented = artifacts.stream()
			.map(a -> augmentArtifact(context, a))
			.collect(Collectors.toList());
		return augmented;
	}

	private static <Context extends ExtractionContext> ArtifactDetails augmentArtifact(Context context, ArtifactDetails artifactdetails) {
		for(Augmenter a : Augmenters.getAugmenters()) {
			artifactdetails = a.augment(context, artifactdetails);
		}
		return artifactdetails;
	}
}
