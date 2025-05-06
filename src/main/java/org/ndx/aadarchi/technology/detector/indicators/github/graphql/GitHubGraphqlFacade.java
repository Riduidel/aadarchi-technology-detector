package org.ndx.aadarchi.technology.detector.indicators.github.graphql;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.logging.Log;
import io.smallrye.graphql.client.GraphQLClient;
import io.smallrye.graphql.client.InvalidResponseException;
import io.smallrye.graphql.client.Response;
import io.smallrye.graphql.client.dynamic.api.DynamicGraphQLClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.ndx.aadarchi.technology.detector.indicators.github.graphql.forks.ForkCountDTO;
import org.ndx.aadarchi.technology.detector.indicators.github.graphql.forks.ForkListDTO;

@ApplicationScoped
public class GitHubGraphqlFacade {
	@Inject
	@GraphQLClient("github")    
	DynamicGraphQLClient dynamicClient;
	
	@ConfigProperty(name = "tech-trends.indicators.github.stars.graphql.today")
	String githubStarsToday;
	@ConfigProperty(name = "tech-trends.indicators.github.stars.graphql.history")
	String githubStarsHistory;

	@ConfigProperty(name = "tech-trends.indicators.github.forks.graphql.today")
	String githubForksToday;
	@ConfigProperty(name = "tech-trends.indicators.github.forks.graphql.history")
	String githubForksHistory;

	/**
	 * Get total number of stargazers as of today
	 * @param owner
	 * @param name
	 * @return number of stargazers
	 * @throws InterruptedException 
	 * @throws ExecutionException 
	 */
	public int getStargazers(String owner, String name) {
		try {
			Map<String, Object> arguments = Map.of(
					"owner", owner,
					"name", name);
			Response response = dynamicClient.executeSync(githubStarsToday, arguments);
			if(response.getErrors()==null || response.getErrors().isEmpty()) {
				return response.getObject(StargazerCountRepository.class, "repository").stargazerCount;
			} else {
				throw processGraphqlErrors(arguments, response);
			}
		} catch (InvalidResponseException | ExecutionException | InterruptedException e) {
			throw new RuntimeException("TODO handle Exception", e);
		}
	}

	private RuntimeException processGraphqlErrors(Map<String, Object> arguments, Response response) {
		return new RuntimeException(
				String.format(
						"Request\n"
						+ "%s\n"
						+ "when executed with parameters %s\n"
						+ "generated errors\n"
						+ "%s", 
						githubStarsToday,
						arguments,
						response.getErrors().stream()
							.map((error -> String.format("\t%s", 
									error.getMessage()))
						).collect(Collectors.joining("\n"))));
	}

	/**
	 * Execute all the requests required to have the whole stargazers set.
	 * So this will run a bunch of requests and process all their results.
	 * @param owner
	 * @param name
	 * @param processStargazers function that will process all received stargazers 
	 * and return true if we must continue (implementation will usually return true if 
	 * at least one was persisted)
	 */
	public void getAllStargazers(String owner, String name, boolean force, Function<StargazerListRepository, Boolean> processStargazers) {
		try {
			Map<String, Object> arguments = new TreeMap<>(Map.of(
					"owner", owner,
					"name", name));
			StargazerListRepository repositoryPage = null;
			boolean shouldContinue = true;
			do {
				Response response = dynamicClient.executeSync(githubStarsHistory, arguments);
				if(response.getErrors()==null || response.getErrors().isEmpty()) {
					repositoryPage = response.getObject(StargazerListRepository.class, "repository");
					shouldContinue = repositoryPage.stargazers.pageInfo.hasPreviousPage;
					boolean hasSavedSomething = processStargazers.apply(repositoryPage);
					if(!force) {
						if(hasSavedSomething) {
						} else {
							Log.infof("We had no new stargazer of %s/%s in this result, stopping", owner, name);
							shouldContinue = false;
						}
					}
					arguments.put("before", repositoryPage.stargazers.pageInfo.startCursor);
				} else {
					throw processGraphqlErrors(arguments, response);
				}
			} while(shouldContinue);
		} catch (InvalidResponseException | ExecutionException | InterruptedException e) {
			throw new RuntimeException("TODO handle Exception", e);
		}
		
	}

	/**
	 * Retrieves the current total number of forks for a repository.
	 * @param owner Repository owner
	 * @param name Repository name
	 * @return Total number of forks
	 */
	public int getCurrentTotalNumberOfFork(String owner, String name) {
		try {
			Map<String, Object> arguments = Map.of(
					"owner", owner,
					"name", name);
			Response response = dynamicClient.executeSync(githubForksToday, arguments);
			if(response.hasData() && (response.getErrors() == null || response.getErrors().isEmpty())) {
				ForkCountDTO repo = response.getObject(ForkCountDTO.class, "repository");
				if (repo != null) {
					return repo.forkCount;
				} else {
					Log.warnf("The GraphQL response for getForkCount(%s, %s) does not contain a 'repository' field. Response: %s\"", owner, name, response.getData());
					return 0;
				}
			} else {
				throw processGraphqlErrors(arguments, response);
			}
		} catch (InvalidResponseException | ExecutionException | InterruptedException e) {
			throw new RuntimeException(String.format("Error retrieving fork count for %s/%s", owner, name), e);
		}
	}

	/**
	 * Retrieves the complete fork history for a repository, page by page.
	 * @param owner Repository owner
	 * @param name Repository name
	 * @param force If true, continues even if a page contains no new processed data.
	 * @param processForks Function to process each received fork page. Must return true if processing should continue.
	 */
	public void getAllForks(String owner, String name, boolean force, Function<ForkListDTO, Boolean> processForks) {
		try {
			Map<String, Object> arguments = new TreeMap<>(Map.of(
					"owner", owner,
					"name", name));
			ForkListDTO repositoryPage;
			boolean shouldContinue = true;
			do {
				Log.debugf("Fetching forks page for %s/%s with arguments: %s", owner, name, arguments);
				Response response = dynamicClient.executeSync(githubForksHistory, arguments);
				if(response.hasData() && (response.getErrors() == null || response.getErrors().isEmpty())) {
					repositoryPage = response.getObject(ForkListDTO.class, "repository");
					if (repositoryPage == null || repositoryPage.forks == null || repositoryPage.forks.pageInfo == null) {
						Log.errorf("Invalid or incomplete response from GraphQL for getAllForks(%s, %s), arguments: %s. Response: %s", owner, name, arguments, response.getData());
						throw new RuntimeException("Incomplete GraphQL response for fork history.");
					}

					shouldContinue = repositoryPage.forks.pageInfo.hasPreviousPage;
					boolean hasProcessedSomething = processForks.apply(repositoryPage);

					if(!force && !hasProcessedSomething) {
						Log.infof("No new forks processed for %s/%s in this page, early shutdown.", owner, name);
						shouldContinue = false;
					}

					if (shouldContinue) {
						Log.debugf("Processing fork page for %s/%s. Next page to fetch before: %s", owner, name, repositoryPage.forks.pageInfo.startCursor);
						arguments.put("before", repositoryPage.forks.pageInfo.startCursor);
					}
				} else {
					Log.debugf("Fork processing complete for %s/%s.", owner, name);
					throw processGraphqlErrors(arguments, response);
				}
			} while(shouldContinue);
		} catch (InvalidResponseException | ExecutionException | InterruptedException e) {
			throw new RuntimeException(String.format("Error retrieving fork history for %s/%s", owner, name), e);
		}
	}

}
