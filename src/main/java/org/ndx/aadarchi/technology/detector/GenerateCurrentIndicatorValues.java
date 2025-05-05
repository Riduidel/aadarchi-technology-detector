package org.ndx.aadarchi.technology.detector;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.camel.builder.endpoint.EndpointRouteBuilder;
import org.ndx.aadarchi.technology.detector.indicators.IndicatorComputer;
import org.ndx.aadarchi.technology.detector.model.TechnologyRepository;
import org.ndx.aadarchi.technology.detector.processors.TechnologyRepositoryProcessor;

import io.quarkus.logging.Log;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@ApplicationScoped
public class GenerateCurrentIndicatorValues extends EndpointRouteBuilder {
	TechnologyRepositoryProcessor technologies;
	
	@Inject Instance<IndicatorComputer> indicatorComputers;

	private List<String> indicatorComputerRoutes;
	
	@PostConstruct
	public void construct() {
		indicatorComputerRoutes = indicatorComputers.stream()
			.map(IndicatorComputer::getFromRouteName)
			.collect(Collectors.toList());
	}

	@Override
	public void configure() throws Exception {
//		Objects.requireNonNull(technologies, "technologies have not been injected. HOW THE FUCK ?!");
		from(direct(getClass().getSimpleName()))
			.id(getClass().getSimpleName()+"-1-fetch-all-technologies")
			.log("Searching for technologies")
			// Load all technologies
			// I think it will be necessary to have some kind of batch processing
			.process(technologies::findAllTechnologies)
			.log("Found ${body.size} technologies")
			.split(body())
				.parallelProcessing()
				.to(direct("compute-all-indicators-for-one-technology-today"))
				.end()
			.log("All indicators have been computed")
			;
		// For each technology, compute all indicators values
		from(direct("compute-all-indicators-for-one-technology-today"))
			.id(getClass().getSimpleName()+"-2-compute-all-indicators-for-one-technology")
			.log("Computing indicators for ${body.name}")
			.multicast()
				.parallelProcessing()
				.recipientList(constant(indicatorComputerRoutes))
				.end()
				.log("Indicators have been computed for ${body.name}")
			;
	}

	@Inject
	public void setTechnologies(TechnologyRepositoryProcessor technologies) {
		this.technologies = technologies;
	}
}
