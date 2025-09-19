package com.zenika.tech.lab.ingester;

import com.zenika.tech.lab.ingester.model.IndicatorComputation;
import com.zenika.tech.lab.ingester.model.Technology;
import com.zenika.tech.lab.ingester.processors.IndicatorComputationProcessor;
import com.zenika.tech.lab.ingester.processors.TechnologyRepositoryProcessor;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.model.ProcessDefinition;
import org.apache.camel.quarkus.test.CamelQuarkusTestSupport;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@QuarkusTest
@TestProfile(ProcessIndicatorComputationsTest.MockingProfile.class)
class ProcessIndicatorComputationsTest extends CamelQuarkusTestSupport {

	@Inject
	ProducerTemplate producerTemplate;

	@Inject
	IndicatorComputationProcessor mockedIndicatorProcessor;

	public static class MockingProfile implements io.quarkus.test.junit.QuarkusTestProfile {
		@Override
		public Set<Class<?>> getEnabledAlternatives() {
			return Collections.singleton(MockedProducers.class);
		}
	}

	@Alternative
	@ApplicationScoped
	static class MockedProducers {
		private static final IndicatorComputationProcessor MOCK_INDICATOR = Mockito.mock(IndicatorComputationProcessor.class);
		private static final TechnologyRepositoryProcessor MOCK_TECH = Mockito.mock(TechnologyRepositoryProcessor.class);

		@Produces
		public IndicatorComputationProcessor indicatorComputationProcessor() {
			return MOCK_INDICATOR;
		}

		@Produces
		public TechnologyRepositoryProcessor technologyRepositoryProcessor() {
			return MOCK_TECH;
		}
	}

	@Override
	public boolean isUseAdviceWith() {
		return true;
	}

	@Test
	void testRoutingToCorrectComputer() throws Exception {

		IndicatorComputation indicatorComputation = new IndicatorComputation();
		IndicatorComputation.IndicatorComputationId id = new IndicatorComputation.IndicatorComputationId();
		Technology technology = new Technology();
		id.technology = technology;
		id.indicatorRoute = "";
		indicatorComputation.id = id;
		List<IndicatorComputation> computations = List.of(indicatorComputation);

		AdviceWith.adviceWith(context, "ProcessIndicatorComputations-1-fetch-all-indicator-computations", a -> {
			a.weaveByType(ProcessDefinition.class).selectFirst()
					.replace().process(exchange -> exchange.getMessage().setBody(computations));
		});

		AdviceWith.adviceWith(context, "route2", a -> {
			a.weaveByType(org.apache.camel.model.ToDynamicDefinition.class).selectFirst()
					.replace().log("Fin de la sous-route de traitement");
		});
		context.start();

		producerTemplate.sendBody("direct:" + ProcessIndicatorComputations.class.getSimpleName(), null);

		verify(mockedIndicatorProcessor, timeout(2000).times(1))
				.markIndicator(
						eq(indicatorComputation),
						eq(IndicatorComputation.IndicatorComputationStatus.HOLD),
						eq(true)
				);
	}

}