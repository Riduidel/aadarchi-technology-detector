package org.ndx.aadarchi.technology.detector.indicators.stackoverflow;

import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.camel.Exchange;
import org.apache.camel.builder.endpoint.EndpointRouteBuilder;
import org.apache.camel.builder.endpoint.dsl.DirectEndpointBuilderFactory.DirectEndpointBuilder;
import org.apache.camel.support.processor.idempotent.MemoryIdempotentRepository;
import org.ndx.aadarchi.technology.detector.indicators.IndicatorComputer;
import org.ndx.aadarchi.technology.detector.model.Technology;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@ApplicationScoped
public class StackOverflowQuestionsIndicatorComputer extends EndpointRouteBuilder implements IndicatorComputer {
	public static final String STACKOVERFLOW_QUESTIONS_COUNT = "stackoverflow.questions.count";
	private static final String ROUTE_NAME = "compute-"+STACKOVERFLOW_QUESTIONS_COUNT.replace('.', '-');
	private @Inject TagsService tagsService;
	
	private DirectEndpointBuilder getFromRoute() {
		return direct(ROUTE_NAME);
	}

	@Override
	public String getFromRouteName() {
		return getFromRoute().getUri();
	}

	@Override
	public void configure() throws Exception {
		from(getFromRoute())
			.routeId(ROUTE_NAME)
			.idempotentConsumer()
				.body(Technology.class, t -> String.format("%s-%s", STACKOVERFLOW_QUESTIONS_COUNT, t.packageManagerUrl))
				.idempotentRepository(MemoryIdempotentRepository.memoryIdempotentRepository(10*2))
			.process(this::countQuestions)
			.end()
		;
	}

	private void countQuestions(Exchange exchange1) {
		countQuestions(exchange1.getMessage().getBody(Technology.class));
	}

	public void countQuestions(Technology body) {
		// First, see if there is a tag linked with technology name
	}

	@Override
	public boolean canCompute(Technology technology) {
		return tagsService.hasTagFor(technology);
	}

}
