package com.zenika.tech.lab.ingester.indicators.github.graphql;

import java.util.concurrent.atomic.AtomicLong;

import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.quarkus.test.CamelQuarkusTestSupport;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class GitHubGraphqlFacadeTest extends CamelQuarkusTestSupport {
    @Inject
    GitHubGraphqlFacade facade;

    @Override
    protected RoutesBuilder createRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() {
                from("direct:start").transform().simple("Hello ${body}").to("mock:result");
            }
        };
    }

    @Test
    void can_count_today_discussion_for_one_popular_project() {
        Assertions.assertThat(facade.getTodayCountAsOfTodayForDiscussions("community", "community"))
                .isGreaterThan(90000);
    }

	@Test
	void can_count_today_forks_for_one_popular_project() {
		Assertions.assertThat(facade.getTodayCountAsOfTodayForForks("microsoft", "TypeScript"))
			.isGreaterThan(100);
	}

    @Test
    void can_count_today_issues_for_one_popular_project() {
        Assertions.assertThat(facade.getTodayCountAsOfTodayForIssues("microsoft", "TypeScript"))
                .isGreaterThan(42000);
    }

    @Test
    void can_count_today_stargazer_for_one_popular_project() {
        Assertions.assertThat(facade.getTodayCountAsOfTodayForStargazers("microsoft", "TypeScript"))
                .isGreaterThan(105000);
    }

    @Test
    void can_get_history_forks_for_one_popular_project() {
        // Given
        AtomicLong forkCount = new AtomicLong();
        // When
        facade.getHistoryCountForForks("microsoft", "TypeScript", false, forkList -> {
            forkCount.addAndGet(forkList.forks().nodes().size());
            return forkCount.longValue() <= 200;
        });
        // Then
        Assertions.assertThat(forkCount.longValue()).isGreaterThan(100);
    }
}
