package com.zenika.tech.lab.ingester.indicators.github;

import org.apache.camel.Exchange;
import org.apache.camel.ExchangePropertyKey;
import org.apache.camel.builder.endpoint.EndpointRouteBuilder;

import com.zenika.tech.lab.ingester.indicators.github.graphql.NoSuchRepository;
import com.zenika.tech.lab.ingester.indicators.github.graphql.RateLimitExceeded;

public abstract class AbstractGitHubEndpointRouteBuilder extends EndpointRouteBuilder  implements GitHubBased {
	String GITHUB_DELAY = "rateLimit.delay";

	public void computeRefillingDelay(Exchange exchange) {
		RateLimitExceeded x = (RateLimitExceeded) exchange.getProperty(ExchangePropertyKey.EXCEPTION_CAUGHT);
		long delay = x.metadata.tokensResetInstant*1000L - System.currentTimeMillis();
		exchange.getMessage().setHeader(GITHUB_DELAY, delay);
	}

	public void configureExceptions() {
		onException(RateLimitExceeded.class)
			.process(this::computeRefillingDelay)
			.delay(simple("${header."+GITHUB_DELAY+"}"))
			.end();
		onException(NoSuchRepository.class)
			.continued(true)
			.end();
	}
}
