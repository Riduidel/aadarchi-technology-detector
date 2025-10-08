package com.zenika.tech.lab.ingester.model;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.zenika.tech.lab.ingester.Configuration;

import io.smallrye.common.annotation.Identifier;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;
import jakarta.transaction.Transactional;
import io.quarkus.logging.Log;
import io.quarkus.panache.common.Parameters;

@ApplicationScoped
public class PriorizedTechnologiesProducer {
	@Produces
	@Transactional
	@Named("priorized")
	List<Technology> buildPriorizedTechnologiesList(
			@ConfigProperty(name=Configuration.CONFIGURATION_PREFIX + "priorized-technologies") List<String> priorized,
			TechnologyRepository technologies) {
		// In a very, VERY, strange fashion, config is not obtained from injection,
		// but from that damn old ServiceProvider thingie
		return priorized.stream()
				.map(technologyIdentifier -> loadTechnology(technologyIdentifier, technologies))
				.flatMap(Optional::stream)
				.collect(Collectors.toList());
	}

	private Optional<Technology> loadTechnology(String value, TechnologyRepository technologies) {
		String[] parts = value.split(":");
		if(parts.length!=2) {
			Log.errorf("configuration property tech-lab-ingester.priorized-technologies contains the invalid value \"\"", value);
			return Optional.empty();
		} else {
			return loadTechnology(parts[0], parts[1], technologies);
		}
		
	}

	private Optional<Technology> loadTechnology(String platform, String name, TechnologyRepository technologies) {
		return technologies.find("platform=:p and name=:n",
				Parameters.with("p", platform).and("n", name)
				).firstResultOptional();
	}
}
