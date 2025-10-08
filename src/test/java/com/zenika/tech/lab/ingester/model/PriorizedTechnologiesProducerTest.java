package com.zenika.tech.lab.ingester.model;

import java.util.List;
import java.util.Optional;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.test.component.QuarkusComponentTest;
import io.quarkus.test.component.QuarkusComponentTestExtension;
import io.quarkus.test.component.TestConfigProperty;
import io.quarkus.test.component.TestConfigProperty.TestConfigProperties;
import io.smallrye.common.annotation.Identifier;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.inject.Named;

class PriorizedTechnologiesProducerTest {
	
    @RegisterExtension
    static final QuarkusComponentTestExtension extension = QuarkusComponentTestExtension
    	.builder()
    	.configProperty("tech-lab-ingester.priorized-technologies", "NPM:react")
    	.mock(TechnologyRepository.class)
    		.scope(Dependent.class)
    		.createMockitoMock(mock -> {
    			PanacheQuery<Technology> mockedQuery = Mockito.mock(PanacheQuery.class);
	    		Mockito.when(mock.find("platform", "NPM", "name", "react"))
	    			.thenReturn(mockedQuery);
	    		Mockito.when(mockedQuery.firstResultOptional()).thenReturn(
    				Optional.of(
	    				TechnologyBuilder.technology()
	    					.platform("NPM")
	    					.name("react")
	    					.build()));
    		})
    	.addComponentClasses(PriorizedTechnologiesProducer.class)
    	.build();
	
	@Inject @Named("priorized") List<Technology> priorized;

	@Test
	void should_have_a_valid_priorized_technologies_list() {
		SoftAssertions.assertSoftly(assertions -> {
			assertions.assertThat(priorized).isNotEmpty();
			assertions.assertThat(priorized)
				.extracting("name")
				.contains("react");
		});
	}

}
