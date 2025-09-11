package com.zenika.tech.lab.ingester.librariesio;

import java.time.temporal.ChronoUnit;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import com.zenika.tech.lab.ingester.librariesio.model.Platform;
import com.zenika.tech.lab.ingester.librariesio.model.Project;

import io.quarkus.cache.CacheResult;
import io.quarkus.rest.client.reactive.ClientQueryParam;
import io.smallrye.faulttolerance.api.RateLimit;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

@RateLimit(value = 60, window = 1, windowUnit = ChronoUnit.MINUTES)
@RegisterRestClient(configKey = "librariesio")
@ClientQueryParam(name="api_key", value="${tech-lab-ingester.libraries.io.token}")
public interface LibrariesIOClient {
	@CacheResult(cacheName="libraries-io-platforms") 
	@GET @Path("/platforms")
	public List<Platform> getPlatforms();
	
	// "https://libraries.io/api/search?api_key={{LIBRARIES_IO_API_KEY}}&page=${exchangeProperty.libraries_io_page_index}&per_page=100&platforms=${exchangeProperty.libraries_io_platform}"
	@GET @Path("/search")
	@CacheResult(cacheName="libraries-io-projects-per-platform") 
	public List<Project> searchProjectsForPlatform(@QueryParam("page") int page, @QueryParam("per_page") int perPage, @QueryParam("platforms") String platform);
	
	@CacheResult(cacheName="libraries-io-search-results") 
	public default List<Project> searchProjectsByText(String text) {
		return searchProjectsByText(100, text);
	}
	public default List<Project> searchProjectsByText(int perPage, String text) {
		boolean hasNextPage = true;
		List<Project> allProjects = new LinkedList<Project>();
    	for (int i = 1; hasNextPage; i++) {
			List<Project> libraries = searchProjectsByText(i, perPage, text, "stars");
			allProjects.addAll(libraries);
			hasNextPage = libraries.size()==perPage;
		}

		return searchProjectsByText(0, perPage, text, "stars");
	}
	/**
	 * 
	 * @param page
	 * @param perPage
	 * @param text
	 * @param sort The search endpoint accepts a sort parameter, one of rank, stars, dependents_count, dependent_repos_count, latest_release_published_at, contributions_count, created_at. 
	 * @return
	 */
	@GET @Path("/search")
	public List<Project> searchProjectsByText(@QueryParam("page") int page, @QueryParam("per_page") int perPage, @QueryParam("q") String text, @QueryParam("sort") String sort);
}
