package org.ndx.aadarchi.technology.detector;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.camel.Exchange;
import org.apache.camel.builder.endpoint.EndpointRouteBuilder;
import org.ndx.aadarchi.technology.detector.indicators.IndicatorComputer;
import org.ndx.aadarchi.technology.detector.model.Technology;
import org.ndx.aadarchi.technology.detector.processors.IndicatorComputationProcessor;
import org.ndx.aadarchi.technology.detector.processors.TechnologyRepositoryProcessor;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@ApplicationScoped
public class GenerateIndicatorComputations extends EndpointRouteBuilder {

	TechnologyRepositoryProcessor technologies;
	IndicatorComputationProcessor indicators;

	@Inject Instance<IndicatorComputer> indicatorComputers;

	private List<String> indicatorComputerRoutes;
	
	@PostConstruct
	public void construct() {
		indicatorComputerRoutes = indicatorComputers.stream()
			.map(IndicatorComputer::getFromRouteName)
			.collect(Collectors.toList());
	}

	@Inject
	public void setTechnologies(TechnologyRepositoryProcessor technologies) {
		this.technologies = technologies;
	}

	@Inject
	public void setIndicators(IndicatorComputationProcessor indicators) {
		this.indicators = indicators;
	}

	@Override
	public void configure() throws Exception {
		from(direct(getClass().getSimpleName()))
			.id(getClass().getSimpleName()+"-1-fetch-all-technologies")
			.log("🔍 Searching for technologies")
			// Load all technologies
			// I think it will be necessary to have some kind of batch processing
			.process(technologies::findAllTechnologies)
			.log("✅  Found ${body.size} technologies")
			.split(body())
				.parallelProcessing()
				.process(this::generateIndicatorComputationsFor)
				.end()
			.log("🎉 All indicators computations have been created, now searching them by date")
			;
	}

	private void generateIndicatorComputationsFor(Exchange exchange) {
		generateIndicatorComputationsFor(exchange.getMessage().getBody(Technology.class));
	}

	private void generateIndicatorComputationsFor(Technology technology) {
		for(String r : indicatorComputerRoutes) {
			indicators.generateIndicatorComputationFor(technology, r);
		}
	}
}
