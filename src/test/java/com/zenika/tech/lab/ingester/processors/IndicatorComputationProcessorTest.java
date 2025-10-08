package com.zenika.tech.lab.ingester.processors;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.quarkus.test.CamelQuarkusTestSupport;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.zenika.tech.lab.ingester.indicators.IndicatorComputer;
import com.zenika.tech.lab.ingester.model.IndicatorComputation;
import com.zenika.tech.lab.ingester.model.IndicatorComputationBuilder;
import com.zenika.tech.lab.ingester.model.IndicatorComputationRepository;
import com.zenika.tech.lab.ingester.model.Technology;
import com.zenika.tech.lab.ingester.model.TechnologyBuilder;
import com.zenika.tech.lab.ingester.model.TechnologyRepository;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@QuarkusTest
class IndicatorComputationProcessorTest extends CamelQuarkusTestSupport {
	
	@Inject TechnologyRepository technologies;
	@Inject IndicatorComputationRepository indicatorComputations;
	@Inject IndicatorComputationProcessor tested;
	@Inject Instance<IndicatorComputer> indicatorComputers;
	private long INDICATORS_COUNT;
	
	@Override
	protected RoutesBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                	.log("Turning off all routes to avoid side effects")
                    .end();
            }
        };
    }
	
	@BeforeEach public void computeIndicatorCount() {
		INDICATORS_COUNT = StreamSupport.stream(indicatorComputers.spliterator(), false).count();
	}

	@Test @Transactional
	void find_all_priorized_should_return_react_first() {
		// Given
		if(indicatorComputations.count()==0) {
			// Create some test data when none exist
			// This is required since we tend to abuse dev mode
			Technology react = TechnologyBuilder.technology()
					.platform("NPM")
					.name("react")
					.build();
			Technology everything = TechnologyBuilder.technology()
					.platform("NPM")
					.name("everything")
					.build();
			technologies.persist(react, everything);
			indicatorComputations.persist(
				IndicatorComputationBuilder.indicatorComputation()
					.technology(react)
					.indicatorRoute("log:just a test")
					.build(),
				IndicatorComputationBuilder.indicatorComputation()
					.technology(react)
					.indicatorRoute("log:just a test")
					.build());
		}
		// When
		Iterable<IndicatorComputation> allIndicatorComputations = tested.findAllPriorized();
		List<IndicatorComputation> indicatorComputations = StreamSupport.stream(allIndicatorComputations.spliterator(), false)
				.limit(10*INDICATORS_COUNT)
				.collect(Collectors.toList());
		// Then
		Assertions.assertThat(indicatorComputations)
			.extracting(i -> i.id.technology.name)
			.contains("react");
	}

}
