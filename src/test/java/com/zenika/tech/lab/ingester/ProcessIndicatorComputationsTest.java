package com.zenika.tech.lab.ingester;

import java.util.List;

import org.apache.camel.ConsumerTemplate;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.model.ProcessDefinition;
import org.apache.camel.quarkus.test.CamelQuarkusTestSupport;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import com.zenika.tech.lab.ingester.model.IndicatorComputation;
import com.zenika.tech.lab.ingester.model.IndicatorComputation.IndicatorComputationStatus;
import com.zenika.tech.lab.ingester.model.IndicatorComputationBuilder;
import com.zenika.tech.lab.ingester.model.Technology;
import com.zenika.tech.lab.ingester.model.TechnologyBuilder;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class ProcessIndicatorComputationsTest extends CamelQuarkusTestSupport {

	@Inject	ProducerTemplate producerTemplate;
	@Inject ConsumerTemplate consumerTemplate;


	@Test
	void testRoutingToCorrectComputer() throws Exception {
		// Given
		Technology TESTED_TECHNOLOGY = TechnologyBuilder.technology()
				.build();
		String INDICATOR_ROUTE = "NO INDICATOR ROUTE CONFIFURED";
		IndicatorComputation indicatorComputation = IndicatorComputationBuilder
				.indicatorComputation()
				.technology(TESTED_TECHNOLOGY)
				.indicatorRoute(INDICATOR_ROUTE)
				.build();
		// Replace standard call (which would get data from database)
		// by our call (which get only one indicator computation)
		AdviceWith.adviceWith(context, ProcessIndicatorComputations.FETCH_ALL_INDICATOR_COMPUTATIONS_ROUTE_ID, a -> {
			a.weaveByType(ProcessDefinition.class).selectFirst()
					.replace().process(exchange -> exchange.getMessage().setBody(List.of(indicatorComputation)));
		});

		context.start();
		String processIndicatorComputationMainRoute = "direct:" + ProcessIndicatorComputations.class.getSimpleName();

		// When
		// We send an empty message to process indicator computations
		Object body = producerTemplate.requestBody(processIndicatorComputationMainRoute, null, Object.class);
		// Then
		Assertions.assertThat(body)
			.asInstanceOf(InstanceOfAssertFactories.LIST)
			.extracting("status")
			.contains(IndicatorComputationStatus.HOLD)
			;
//		verify(mockedIndicatorProcessor, timeout(2000).times(1))
//				.markIndicator(
//						eq(indicatorComputation),
//						eq(IndicatorComputation.IndicatorComputationStatus.HOLD),
//						eq(true)
//				);
	}

}