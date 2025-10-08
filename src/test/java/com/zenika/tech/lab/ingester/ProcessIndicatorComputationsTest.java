package com.zenika.tech.lab.ingester;

import java.util.List;

import org.apache.camel.ConsumerTemplate;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.model.ProcessDefinition;
import org.apache.camel.quarkus.test.CamelQuarkusTestSupport;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.zenika.tech.lab.ingester.model.IndicatorComputation;
import com.zenika.tech.lab.ingester.model.IndicatorComputation.IndicatorComputationStatus;
import com.zenika.tech.lab.ingester.model.IndicatorComputationBuilder;
import com.zenika.tech.lab.ingester.model.IndicatorComputationRepository;
import com.zenika.tech.lab.ingester.model.Technology;
import com.zenika.tech.lab.ingester.model.TechnologyBuilder;
import com.zenika.tech.lab.ingester.model.TechnologyRepository;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class ProcessIndicatorComputationsTest extends CamelQuarkusTestSupport {

	@Inject	ProducerTemplate producerTemplate;
	@Inject ConsumerTemplate consumerTemplate;
	@Inject IndicatorComputationRepository indicatorsToMock;

	static Technology TESTED_TECHNOLOGY = TechnologyBuilder.technology()
			.build();
	static String INDICATOR_ROUTE = "log:done with that fake indicator route";
	static IndicatorComputation indicatorComputation = IndicatorComputationBuilder
			.indicatorComputation()
			.technology(TESTED_TECHNOLOGY)
			.indicatorRoute(INDICATOR_ROUTE)
			.build();

	@BeforeAll
	public static void setup() {
		IndicatorComputationRepository mockingIndicators = Mockito.mock(IndicatorComputationRepository.class);
		PanacheQuery mockedQuery = Mockito.mock(PanacheQuery.class);
		Mockito.when(mockedQuery.firstResult()).thenReturn(indicatorComputation);
		Mockito
			.when(mockingIndicators.find(Mockito.anyString(), 
					Mockito.<Object[]>any() ))
			.thenReturn(mockedQuery);
		QuarkusMock.installMockForType(mockingIndicators, IndicatorComputationRepository.class);
	}

	@Test
	void testRoutingToCorrectComputer() throws Exception {
		// Given
		
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