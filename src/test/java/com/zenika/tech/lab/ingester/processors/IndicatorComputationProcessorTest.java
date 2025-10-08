package com.zenika.tech.lab.ingester.processors;

import static org.assertj.core.api.Assertions.from;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.quarkus.test.CamelQuarkusTestSupport;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.zenika.tech.lab.ingester.indicators.IndicatorComputer;
import com.zenika.tech.lab.ingester.model.IndicatorComputation;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@QuarkusTest
class IndicatorComputationProcessorTest extends CamelQuarkusTestSupport {
	
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
