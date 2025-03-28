package org.ndx.aadarchi.technology.detector.model;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.inject.Inject;

@ApplicationScoped
public class IndicatorRepositoryFacadeProducer {
	@Inject IndicatorRepository repository;
	@Produces
	@IndicatorNamed
	IndicatorRepositoryFacade createFacadeFor(InjectionPoint injectionPoint) {
		return injectionPoint.getQualifiers().stream()
			.filter(a -> a instanceof IndicatorNamed)
			.map(a -> (IndicatorNamed) a)
			.map(named -> new IndicatorRepositoryFacade(repository, named.value()))
			.findFirst()
			.orElseThrow(() -> new RuntimeException("There should be a @Named annotation in "+injectionPoint));
	}
}
