package org.ndx.aadarchi.technology.detector.indicators.github.graphql;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.faulttolerance.Retry;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.BucketListener;
import io.github.bucket4j.TokensInheritanceStrategy;
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

	public class BucketThreadParkedLogger implements BucketListener {

	@ConfigProperty(name = "tech-trends.indicators.github.forks.graphql.today")
	String githubForksToday;
	@ConfigProperty(name = "tech-trends.indicators.github.forks.graphql.history")
	String githubForksHistory;

		@Override
		public void onConsumed(long tokens) {}

		@Override
		public void onRejected(long tokens) {}

		@Override
		public void beforeParking(long nanos) {
			Log.warnf("Thread will be parked %s due to bucket having only %d tokens remaining",
					DurationFormatUtils.formatDurationHMS(TimeUnit.NANOSECONDS.toMillis(nanos)),
					rateLimitingBucket.getAvailableTokens());
		}

		@Override
		public void onParked(long nanos) {
			Log.warnf("Thread was parked %s due to bucket having only %d tokens remaining. Operations resume NOW!",
					DurationFormatUtils.formatDurationHMS(TimeUnit.NANOSECONDS.toMillis(nanos)),
					rateLimitingBucket.getAvailableTokens());
		}

		@Override
		public void onInterrupted(InterruptedException e) {}

		@Override
		public void onDelayed(long nanos) {}

	}

	Bucket rateLimitingBucket = Bucket.builder()
			// We initialize as a classical GitHub user
			.addLimit(limit -> limit
					.capacity(5_000)
					.refillIntervally(5_000, Duration.ofHours(1))
					.id("GitHub")
				)
			.withMillisecondPrecision()
			.build()
			.toListenable(new BucketThreadParkedLogger());

	/**
	 * Get total number of stargazers as of today
	 * @param owner
	 * @param name
	 * @return number of stargazers
	 * @throws InterruptedException 
	 * @throws ExecutionException 
	 */
	@Retry(maxRetries = 3)
	public int getStargazers(String owner, String name) {
		try {
			Map<String, Object> arguments = Map.of(
					"owner", owner,
					"name", name);
			Response response = executeSync(githubStarsToday, arguments, 1);
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
		return new GitHubGraphqlException(
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
	@Retry(maxRetries = 3)
	public void getAllStargazers(String owner, String name, boolean force, Function<StargazerListRepository, Boolean> processStargazers) {
		try {
			Map<String, Object> arguments = new TreeMap<>(Map.of(
					"owner", owner,
					"name", name));
			StargazerListRepository repositoryPage = null;
			boolean shouldContinue = true;
			do {
				Response response = executeSync(githubStarsHistory, arguments, 100);
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

	private Response executeSync(String query, Map<String, Object> arguments, long tokens) throws ExecutionException, InterruptedException {
		rateLimitingBucket.asBlocking().consume(tokens);
		Response returned = dynamicClient.executeSync(query, arguments);
		Map<String, String> metadata = returned.getTransportMeta()
				.entrySet()
				.stream()
				.collect(Collectors.toMap(
						entry -> entry.getKey().toLowerCase(),
						entry -> entry.getValue().stream().collect(Collectors.joining())));
		int tokensPerHour = Integer.parseInt(metadata.get("x-ratelimit-limit"));
		int tokensRemaining = Integer.parseInt(metadata.get("x-ratelimit-remaining"));
		int tokensUsed = Integer.parseInt(metadata.get("x-ratelimit-used"));
		int tokensResetInstant = Integer.parseInt(metadata.get("x-ratelimit-reset"));
		Instant resetInstant = Instant.ofEpochSecond(tokensResetInstant);
		if(tokensRemaining<100) {
			Log.warnf("%s tokens remaining. Bucket refulling will happen at %s",
					tokensRemaining,
					resetInstant.atZone(ZoneId.systemDefault())
					);
		} else {
			Log.debugf("%s tokens remaining locally, and %s tokens remaining on GitHub side.",
					rateLimitingBucket.getAvailableTokens(),
					tokensRemaining);
		}
		rateLimitingBucket.replaceConfiguration(
				BucketConfiguration.builder()
					.addLimit(limit ->
						limit.capacity(tokensRemaining)
							.refillIntervallyAligned(tokensPerHour, Duration.ofHours(1), resetInstant)
					)
					.build(),
				TokensInheritanceStrategy.RESET);
		return returned;
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
				throw processGraphqlErrors(githubForksToday, arguments, response);
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
					throw processGraphqlErrors(githubForksHistory, arguments, response);
				}
			} while(shouldContinue);
		} catch (InvalidResponseException | ExecutionException | InterruptedException e) {
			throw new RuntimeException(String.format("Erreur lors de la récupération de l'historique des forks pour %s/%s", owner, name), e);
		}
	}

	/**
	 * Calcule le nombre de forks par mois pour un dépôt donné.
	 * @param owner Propriétaire du dépôt
	 * @param name Nom du dépôt
	 * @return Une Map où la clé est l'année/mois (YearMonth) et la valeur est le nombre de forks créés ce mois-là.
	 */
	public Map<YearMonth, Long> getForksPerMonth(String owner, String name) {
		List<OffsetDateTime> allForkDates = new ArrayList<>();

		// Utilise getAllForks pour récupérer toutes les dates de création des forks
		getAllForks(owner, name, true, // force=true pour obtenir toutes les dates, même anciennes
				repositoryPage -> {
					if (repositoryPage.forks != null && repositoryPage.forks.nodes != null) {
						List<OffsetDateTime> datesOnPage = repositoryPage.forks.nodes.stream()
								.map(forkNode -> forkNode.createdAt)
								.collect(Collectors.toList());
						allForkDates.addAll(datesOnPage);
						return !datesOnPage.isEmpty();
					}
					return false;
				});

		Log.infof("Total de %d dates de fork récupérées pour %s/%s.", allForkDates.size(), owner, name);

		// Groupe les dates par année/mois et compte les occurrences
		return allForkDates.stream()
				.collect(Collectors.groupingBy(
						date -> YearMonth.from(date),
						TreeMap::new,
						Collectors.counting()
				));
	}


	/**
	 * Traite les erreurs GraphQL et lance une RuntimeException.
	 * @param query La requête GraphQL qui a échoué.
	 * @param arguments Les arguments utilisés pour la requête.
	 * @param response La réponse GraphQL contenant les erreurs.
	 * @return Ne retourne jamais, lance toujours une exception.
	 */
	private RuntimeException processGraphqlErrors(String query, Map<String, Object> arguments, Response response) {
		String errorDetails = "Erreurs inconnues";
		if (response != null && response.getErrors() != null && !response.getErrors().isEmpty()) {
			errorDetails = response.getErrors().stream()
					.map(error -> String.format("\t- %s (Locations: %s, Path: %s, Extensions: %s)",
							error.getMessage(), error.getLocations(), error.getPath(), error.getExtensions()))
					.collect(Collectors.joining("\n"));
		} else if (response != null && !response.hasData()) {
			errorDetails = "La réponse ne contient pas de données ('data' est null ou vide).";
		}

		return new RuntimeException(
				String.format(
						"La requête GraphQL\n"
								+ "--------------------\n"
								+ "%s\n"
								+ "--------------------\n"
								+ "exécutée avec les paramètres %s\n"
								+ "a généré les erreurs suivantes:\n"
								+ "%s",
						query,
						arguments,
						errorDetails));
	}

}
