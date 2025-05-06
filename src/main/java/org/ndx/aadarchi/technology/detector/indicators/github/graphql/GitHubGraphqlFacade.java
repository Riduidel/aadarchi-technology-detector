package org.ndx.aadarchi.technology.detector.indicators.github.graphql;

import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
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
	 * Récupère le nombre total actuel de forks pour un dépôt.
	 * @param owner Propriétaire du dépôt
	 * @param name Nom du dépôt
	 * @return Nombre total de forks
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
					Log.warnf("La réponse GraphQL pour getForkCount(%s, %s) ne contient pas de champ 'repository'. Réponse: %s", owner, name, response.getData());
					return 0;
				}
			} else {
				throw processGraphqlErrors(arguments, response);
			}
		} catch (InvalidResponseException | ExecutionException | InterruptedException e) {
			throw new RuntimeException(String.format("Erreur lors de la récupération du nombre de forks pour %s/%s", owner, name), e);
		}
	}

	/**
	 * Récupère l'historique complet des forks pour un dépôt, page par page.
	 * @param owner Propriétaire du dépôt
	 * @param name Nom du dépôt
	 * @param force Si true, continue même si une page ne contient pas de nouvelles données traitées.
	 * @param processForks Fonction pour traiter chaque page de forks reçue. Doit retourner true si le traitement doit continuer.
	 */
	public void getAllForks(String owner, String name, boolean force, Function<ForkListDTO, Boolean> processForks) {
		try {
			Map<String, Object> arguments = new TreeMap<>(Map.of(
					"owner", owner,
					"name", name));
			ForkListDTO repositoryPage;
			boolean shouldContinue = true;
			do {
				Log.debugf("Récupération de la page de forks pour %s/%s avec arguments: %s", owner, name, arguments);
				Response response = dynamicClient.executeSync(githubForksHistory, arguments);
				if(response.hasData() && (response.getErrors() == null || response.getErrors().isEmpty())) {
					repositoryPage = response.getObject(ForkListDTO.class, "repository");
					if (repositoryPage == null || repositoryPage.forks == null || repositoryPage.forks.pageInfo == null) {
						Log.errorf("Réponse invalide ou incomplète de GraphQL pour getAllForks(%s, %s), arguments: %s. Réponse: %s", owner, name, arguments, response.getData());
						throw new RuntimeException("Réponse GraphQL incomplète pour l'historique des forks.");
					}

					shouldContinue = repositoryPage.forks.pageInfo.hasPreviousPage;
					boolean hasProcessedSomething = processForks.apply(repositoryPage);

					if(!force && !hasProcessedSomething) {
						Log.infof("Aucun nouveau fork traité pour %s/%s dans cette page, arrêt anticipé.", owner, name);
						shouldContinue = false;
					}

					if (shouldContinue) {
						Log.debugf("Page de forks traitée pour %s/%s. Page suivante à récupérer avant: %s", owner, name, repositoryPage.forks.pageInfo.startCursor);
						arguments.put("before", repositoryPage.forks.pageInfo.startCursor);
					}
				} else {
					Log.debugf("Traitement des forks terminé pour %s/%s.", owner, name);
					throw processGraphqlErrors(arguments, response);
				}
			} while(shouldContinue);
		} catch (InvalidResponseException | ExecutionException | InterruptedException e) {
			throw new RuntimeException(String.format("Erreur lors de la récupération de l'historique des forks pour %s/%s", owner, name), e);
		}
	}

}
