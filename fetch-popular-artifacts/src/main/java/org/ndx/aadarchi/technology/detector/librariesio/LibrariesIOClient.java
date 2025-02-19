package org.ndx.aadarchi.technology.detector.librariesio;

import java.util.List;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.ndx.aadarchi.technology.detector.librariesio.model.Platform;
import org.ndx.aadarchi.technology.detector.librariesio.model.Project;

import io.quarkiverse.bucket4j.runtime.RateLimited;
import io.quarkus.cache.CacheResult;
import io.quarkus.rest.client.reactive.ClientQueryParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

@RateLimited(bucket = "librariesio")
@RegisterRestClient(configKey = "librariesio")
@ClientQueryParam(name="api_key", value="${LIBRARIES_IO_API_KEY}")
public interface LibrariesIOClient {
	@CacheResult(cacheName="libraries-io-platforms") 
	@GET @Path("/platforms")
	public List<Platform> getPlatforms();
	
	// "https://libraries.io/api/search?api_key={{LIBRARIES_IO_API_KEY}}&page=${exchangeProperty.libraries_io_page_index}&per_page=100&platforms=${exchangeProperty.libraries_io_platform}"
	@GET @Path("/search")
	public List<Project> searchProjects(@QueryParam("page") int page, @QueryParam("per_page") int perPage, @QueryParam("platforms") String platform);
}
