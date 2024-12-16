package org.ndx.aadarchi.technology.detector.augmenters.github;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.ndx.aadarchi.technology.detector.augmenters.Augmenter;
import org.ndx.aadarchi.technology.detector.exceptions.CannotWriteToCache;
import org.ndx.aadarchi.technology.detector.exceptions.CannottReadFromCache;
import org.ndx.aadarchi.technology.detector.helper.FileHelper;
import org.ndx.aadarchi.technology.detector.loader.ExtractionContext;
import org.ndx.aadarchi.technology.detector.model.ArtifactDetails;
import org.ndx.aadarchi.technology.detector.model.GitHubDetails;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;

import io.github.emilyydev.asp.ProvidesService;

@ProvidesService(Augmenter.class)
public class AddGitHubStarsAtPeriod implements Augmenter {
	private static final Logger logger = Logger.getLogger(AddGitHubStarsAtPeriod.class.getName());
	
	@Override
	public int order() {
		return AddGitHub.ADD_GITHUB_OBJECT+10;
	}

	@Override
	public ArtifactDetails augment(ExtractionContext context, ArtifactDetails source, LocalDate date) {
		if(source.getGithubDetails()!=null) {
			// Make sure repository exist before anything
			return doAugment(context, source, date);
		}
		return source;
	}
	
	private ArtifactDetails doAugment(ExtractionContext context, ArtifactDetails source, LocalDate date) {
		GitHubDetails githubDetails = source.getGithubDetails();
		if(githubDetails.getStargazers().isEmpty()) {
			// We have a special edge case to distinguish between 
			// history rebuilding and standard data fetching.
			// When getting this month stargazers, it's way faster to ask
			// GitHub directly instead of getting the precise list of stargazers
			LocalDate now = LocalDate.now();
			Period period = Period.between(date, now);
			if(period.toTotalMonths()>0) {
				extractStargazersHistory(context, source, date, githubDetails);
			} else {
				extractStargazersToday(context, source, githubDetails);
			}
		}
		return source;
	}

	private void extractStargazersToday(ExtractionContext context, ArtifactDetails source, GitHubDetails githubDetails) {
		githubDetails.setStargazers(Optional.of(getCurrentStargazersCount(context.getGithubToken(), githubDetails)));
	}

	Integer getCurrentStargazersCount(String githubToken, GitHubDetails githubDetails) {
		GitHubGraphQLClient helper = GitHubGraphQLClient.getClient(githubToken);
		JsonNode response = helper.runGraphQLQuery(GitHubGraphQLClient.STARGAZERS_COUNT, githubDetails.getOwner(), githubDetails.getRepository());
		JsonNode data = response.get("data");
		JsonNode repository = data
			.get("repository");
		JsonNode stargazerCount = repository
				.get("stargazerCount");
		if(stargazerCount==null) {
			return 0;
		} else {
			return stargazerCount.asInt();
		}
	}

	void extractStargazersHistory(ExtractionContext context, ArtifactDetails source, LocalDate date,
			GitHubDetails githubDetails) {
		Date old = Date.from(date.atStartOfDay(ZoneOffset.UTC).toInstant());
		Collection<Stargazer> allStargazers = getAllStargazers(context.getGithubToken(), context, source, githubDetails);
		long numberOfStargazersBefore = allStargazers.stream()
				.filter(s -> s.getStarredAt().compareTo(old)<0)
				.count();
		if(numberOfStargazersBefore>0) {
			githubDetails.setStargazers(Optional.of((int) numberOfStargazersBefore));
		} else {
			githubDetails.setStargazers(Optional.empty());
		}
	}

	Collection<Stargazer> getAllStargazers(String githubToken, ExtractionContext context, ArtifactDetails source, GitHubDetails details) {
		try {
			int total = getCurrentStargazersCount(githubToken, details);
			File cache = context.getCache()
					.resolve("github")
					.resolve(details.getPath())
					.resolve("stargazers.json")
					.toFile();
			cache.getParentFile().mkdirs();
			int percentage = 0;
			Collection<Stargazer> returned = new TreeSet<Stargazer>();
			if(cache.exists()) {
				returned = FileHelper.readFromFile(cache, new TypeReference<TreeSet<Stargazer>>() {});
				percentage = (int) (returned.size()/((float) total)*100);
			}
			if(percentage>0 && percentage<90) {
				logger.warning(
						String.format("Our cache doesn't contains enough data for %S (it contains only %d %%). Force refreshing",
								details.getPath(), percentage));
				cache.delete();
				returned.clear();
			}
			if(!cache.exists() || cache.lastModified()<System.currentTimeMillis()-Duration.ofDays(7).toMillis()) {
				Collection<Stargazer> stargazers = doGetAllStargazers(githubToken, details, total, returned);
				try {
					FileHelper.writeToFile(stargazers, cache);
				} catch (IOException e) {
					throw new CannotWriteToCache("Can't write stargazers to "+cache.getAbsolutePath(), e);
				}
				returned = FileHelper.readFromFile(cache, new TypeReference<TreeSet<Stargazer>>() {});
			}
			return returned;
		} catch (IOException e) {
			throw new CannottReadFromCache("Can't read stargazers", e);
		}
	}

	Collection<Stargazer> doGetAllStargazers(String githubToken, GitHubDetails githubDetails, int total, Collection<Stargazer> initial) {
		String repositoryOwner = githubDetails.getOwner();
		String repositoryName = githubDetails.getRepository();
		try {
			Collection<Stargazer> returned = new TreeSet<Stargazer>(initial);
			String cursor = "";
			boolean alreadyKnown = false;
			do {
				GitHubGraphQLClient helper = GitHubGraphQLClient.getClient(githubToken);
				JsonNode response = helper.runGraphQLQuery(GitHubGraphQLClient.STARGAZERS_LIST, repositoryOwner, repositoryName, cursor);
				if(response.has("errors")) {
					return returned;
				}
				cursor = getNextCursorValue(response);
				// Now process data
				JsonNode data = response.get("data");
				JsonNode repository = data.get("repository");
				JsonNode stargazers = repository.get("stargazers");
				TreeSet<Stargazer> newStargazers = StreamSupport.stream(stargazers.get("edges").spliterator(), false)
					.map(this::toStargazer)
					.collect(Collectors.toCollection(() -> new TreeSet<>()));
				 alreadyKnown = !returned.addAll(newStargazers);
				logger.info("Fetched "+returned.size()+"/"+total+" stargazers of "+repositoryOwner+"/"+repositoryName);
			} while(cursor!=null && !cursor.equals("") && !alreadyKnown);
			return returned;
		} catch (CannotPerformGraphQL e) {
			throw new CannotFetchStargazers("Cannot fetch stargazers of "+repositoryOwner+"/"+repositoryName, e);
		} finally {
			logger.info("Fetched stargazers history of "+repositoryOwner+"/"+repositoryName);
		}
	}

	private Stargazer toStargazer(JsonNode element) {
		String starredAtText = element.get("starredAt").asText();
		String login = element.get("node").get("login").asText();
		LocalDateTime starredAt = LocalDateTime.parse(starredAtText, DateTimeFormatter.ISO_DATE_TIME);
		return new Stargazer(Date.from(starredAt.toInstant(ZoneOffset.UTC)), login);
	}

	private String getNextCursorValue(JsonNode response) {
		JsonNode data = response.get("data");
		JsonNode repository = data.get("repository");
		JsonNode stargazers = repository.get("stargazers");
		JsonNode pageInfo = stargazers.get("pageInfo");
		if(pageInfo.get("hasNextPage").asBoolean()) {
			return pageInfo.get("endCursor").asText();
		} else {
			return null;
		}
	}

}
