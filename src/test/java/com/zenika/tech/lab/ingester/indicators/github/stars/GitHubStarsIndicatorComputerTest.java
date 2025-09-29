package com.zenika.tech.lab.ingester.indicators.github.stars;

import com.zenika.tech.lab.ingester.indicators.github.graphql.GitHubGraphqlFacade;
import com.zenika.tech.lab.ingester.indicators.github.graphql.NoSuchRepository;
import com.zenika.tech.lab.ingester.model.IndicatorRepository;
import com.zenika.tech.lab.ingester.model.Technology;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.quarkus.test.CamelQuarkusTestSupport;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;

@QuarkusTest
class GitHubStarsIndicatorComputerTest extends CamelQuarkusTestSupport {

	@Inject
	ProducerTemplate producerTemplate;

	@InjectMock
	GitHubGraphqlFacade gitHubGraphqlFacade;

	@InjectMock
	IndicatorRepository indicatorRepository;

	@Test
	public void should_not_persist_indicator_when_repository_is_not_found() throws Exception {
		//Given
		Technology technology = new Technology();
		technology.repositoryUrl = "https://cs.opensource.google/go/x/crypto";

		Mockito.when(gitHubGraphqlFacade.getTodayCountAsOfTodayForStargazers("go", "x"))
				.thenThrow(new NoSuchRepository("Could not resolve to a Repository with the name 'go/x'.", null));

		// When
		producerTemplate.sendBody("direct:compute-github-stars", technology);

		// Then
		Mockito.verify(indicatorRepository, Mockito.times(0)).maybePersist(any());
	}

}