package org.ndx.aadarchi.technology.detector.mappers;

import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class Mappers {
	 private static ServiceLoader<MappingGenerator> mapperLoader
  	= ServiceLoader.load(MappingGenerator.class);
	 private static Optional<List<MappingGenerator>> loaded = Optional.empty();
	 
	 public static List<MappingGenerator> getMappers() {
		 if(loaded.isEmpty())
		 	loaded = Optional.of(StreamSupport.stream(mapperLoader.spliterator(), false)
				 .collect(Collectors.toList()));
		 return loaded.get();
	 }

}
