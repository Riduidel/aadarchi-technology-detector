package org.ndx.aadarchi.technology.detector.librariesio;

import java.time.temporal.ChronoUnit;
import java.util.List;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.ndx.aadarchi.technology.detector.librariesio.model.Platform;
import org.ndx.aadarchi.technology.detector.librariesio.model.Project;

import io.quarkus.cache.CacheResult;
import io.quarkus.rest.client.reactive.ClientQueryParam;
import io.smallrye.faulttolerance.api.RateLimit;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

@RateLimit(value = 60, window = 1, windowUnit = ChronoUnit.MINUTES)
@RegisterRestClient(configKey = "librariesio")
@ClientQueryParam(name="api_key", value="${tech-trends.libraries.io.token}")
public interface LibrariesIOClient {
	@CacheResult(cacheName="libraries-io-platforms") 
	@GET @Path("/platforms")
	public List<Platform> getPlatforms();
	
	// "https://libraries.io/api/search?api_key={{LIBRARIES_IO_API_KEY}}&page=${exchangeProperty.libraries_io_page_index}&per_page=100&platforms=${exchangeProperty.libraries_io_platform}"
	@GET @Path("/search")
	public List<Project> searchProjects(@QueryParam("page") int page, @QueryParam("per_page") int perPage, @QueryParam("platforms") String platform);
}
