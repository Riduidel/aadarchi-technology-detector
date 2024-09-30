package org.ndx.aadarchi.technology.detector.augmenters;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

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
}
