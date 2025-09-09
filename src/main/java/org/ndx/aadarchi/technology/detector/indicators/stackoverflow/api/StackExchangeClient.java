package org.ndx.aadarchi.technology.detector.indicators.stackoverflow.api;

import java.time.temporal.ChronoUnit;
import java.util.Date;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.RestQuery;

import io.quarkus.rest.client.reactive.ClientQueryParam;
import io.smallrye.faulttolerance.api.RateLimit;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@RateLimit(value = 60, window = 1, windowUnit = ChronoUnit.MINUTES)
@RegisterRestClient(configKey = "stackexchange")
@ClientQueryParam(name="key", value="${tech-trendz.stackexchange.api.key}")
@Path("/2.3")
public interface StackExchangeClient {
	@GET @Path("/tags")
	// We have to make sure the last activity date is returned
	@ClientQueryParam(name = "filter", value = "total") 
	public StackExchangeList<Tag> getTags(
			@RestQuery String site,
			@RestQuery int page, 
			@RestQuery int pagesize, 
			@RestQuery Date fromdate,
			@RestQuery Date todate);
}
