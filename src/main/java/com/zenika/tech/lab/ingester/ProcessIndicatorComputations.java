package com.zenika.tech.lab.ingester;

import com.zenika.tech.lab.ingester.indicators.IndicatorComputer;
import com.zenika.tech.lab.ingester.model.IndicatorComputation;
import com.zenika.tech.lab.ingester.processors.IndicatorComputationProcessor;
import com.zenika.tech.lab.ingester.processors.TechnologyRepositoryProcessor;
import io.quarkus.logging.Log;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.endpoint.EndpointRouteBuilder;
import org.apache.camel.builder.endpoint.dsl.DirectEndpointBuilderFactory.DirectEndpointBuilder;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@ApplicationScoped
public class ProcessIndicatorComputations extends EndpointRouteBuilder  {
	private static final String INDICATOR_ROUTE_HEADER = "route";
	TechnologyRepositoryProcessor technologies;
	IndicatorComputationProcessor indicators;
	
	@Inject Instance<IndicatorComputer> indicatorComputers;

	private Map<String, IndicatorComputer> indicatorComputerRoutes;
	
	@PostConstruct
	public void construct() {
		indicatorComputerRoutes = indicatorComputers.stream()
				.collect(Collectors.toMap(IndicatorComputer::getFromRouteName, Function.identity()))
				;
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
		DirectEndpointBuilder PROCESS_ONE_TECHNOLOGY = direct(getClass().getSimpleName()+"-process-one-technology");
		from(direct(getClass().getSimpleName()))
			.id(getClass().getSimpleName()+"-1-fetch-all-indicator-computations")
			.log("🔍 Searching for indicator computations")
			// I think it will be necessary to have some kind of batch processing
			.process(this::findAllOldestFirst)
			.log("✅ Found ${body.size} technologies")
			.split(body())
				.parallelProcessing()
					.to(PROCESS_ONE_TECHNOLOGY)
				.end()
			.end()
			.log("🎉 All indicators computations have been processed")
			;
		from(PROCESS_ONE_TECHNOLOGY)
			.onCompletion().onCompleteOnly()
			.	process(e -> convertBackToIndicatorComputation(e, true))
			.end()
			.onCompletion().onFailureOnly()
				.process(e -> convertBackToIndicatorComputation(e, false))
			.end()
			.choice()
				.when(this::canComputeIndicator)
					.log("Running ${header.CamelSplitIndex}/${header.CamelSplitSize} ${body}")
					// Mark the indicator computation as LOADED
					.process(this::convertToTechnology)
					// Dynamically route it
					.toD("${header." + INDICATOR_ROUTE_HEADER + "}")
			.otherwise()
				.log(LoggingLevel.WARN, "Cannot currently compute ${body}")
			.endChoice()
		.end();
	}
	public void findAllOldestFirst(Exchange exchange) {
		exchange.getMessage().setBody(indicators.findAllOldestFirst());
	}
	public boolean canComputeIndicator(Exchange exchange) {
		IndicatorComputation indicator = exchange.getMessage().getBody(IndicatorComputation.class);
		IndicatorComputer computer = indicatorComputerRoutes.get(indicator.id.indicatorRoute);
		if(computer==null) {
			Log.errorf("🛑 Indicator computation %s routes to missing indicator computer %s. It won't be computed AT ALL",
					indicator,
					indicator.id.indicatorRoute);
		} else {
			return computer.canCompute(indicator.id.technology);
		}
		return false;
	}
	
	public void convertToTechnology(Exchange exchange) {
		IndicatorComputation indicator = exchange.getMessage().getBody(IndicatorComputation.class);
		indicators.markIndicator(indicator, IndicatorComputation.IndicatorComputationStatus.LOADED, false);
		exchange.getMessage().setHeader("indicatorComputation", indicator);
		exchange.getMessage().setHeader(INDICATOR_ROUTE_HEADER, indicator.id.indicatorRoute);
		exchange.getMessage().setBody(indicator.id.technology);
	}
	public void convertBackToIndicatorComputation(Exchange exchange, boolean updateDate) {
		IndicatorComputation indicatorComputation = exchange.getMessage().getHeader("indicatorComputation", IndicatorComputation.class);
		indicators.markIndicator(indicatorComputation, IndicatorComputation.IndicatorComputationStatus.HOLD, updateDate);
		exchange.getMessage().setBody(indicatorComputation);
	}
}
