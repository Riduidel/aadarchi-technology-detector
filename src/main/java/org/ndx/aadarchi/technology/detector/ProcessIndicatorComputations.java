package org.ndx.aadarchi.technology.detector;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.camel.Exchange;
import org.apache.camel.builder.endpoint.EndpointRouteBuilder;
import org.ndx.aadarchi.technology.detector.indicators.IndicatorComputer;
import org.ndx.aadarchi.technology.detector.model.IndicatorComputation;
import org.ndx.aadarchi.technology.detector.model.Technology;
import org.ndx.aadarchi.technology.detector.processors.IndicatorComputationProcessor;
import org.ndx.aadarchi.technology.detector.processors.TechnologyRepositoryProcessor;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@ApplicationScoped
public class ProcessIndicatorComputations extends EndpointRouteBuilder  {
	private static final String INDICATOR_ROUTE_HEADER = "route";
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
			.id(getClass().getSimpleName()+"-1-fetch-all-indicator-computations")
			.log("Searching for indicator computations")
			// I think it will be necessary to have some kind of batch processing
			.process(this::findAllOldestFirst)
			.log("Found ${body.size} technologies")
			.split(body())
				.parallelProcessing()
				// Mark the indicator computation as LOADED
				.process(this::convertToTechnology)
				// Dynamically route it
				.toD("${header."+INDICATOR_ROUTE_HEADER+"}")
				.process(this::convertBackToIndicatorComputation)
				.end()
			.log("All indicators computations have been processed")
			;
	}
	public void findAllOldestFirst(Exchange exchange) {
		exchange.getMessage().setBody(indicators.findAllOldestFirst());
	}
	
	public void convertToTechnology(Exchange exchange) {
		IndicatorComputation indicator = exchange.getMessage().getBody(IndicatorComputation.class);
		indicators.markIndicator(indicator, IndicatorComputation.IndicatorComputationStatus.LOADED, false);
		exchange.getMessage().setHeader("indicatorComputation", indicator);
		exchange.getMessage().setHeader(INDICATOR_ROUTE_HEADER, indicator.id.indicatorRoute);
		exchange.getMessage().setBody(indicator.id.technology);
	}
	public void convertBackToIndicatorComputation(Exchange exchange) {
		IndicatorComputation indicatorComputation = exchange.getMessage().getHeader("indicatorComputation", IndicatorComputation.class);
		indicators.markIndicator(indicatorComputation, IndicatorComputation.IndicatorComputationStatus.HOLD, true);
		exchange.getMessage().setBody(indicatorComputation);
	}
}
