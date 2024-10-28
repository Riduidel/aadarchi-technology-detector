package org.ndx.aadarchi.technology.detector.augmenters.github;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.text.DateFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.stream.StreamSupport;

import org.ndx.aadarchi.technology.detector.augmenters.Augmenter;
import org.ndx.aadarchi.technology.detector.exceptions.CannotWriteToCache;
import org.ndx.aadarchi.technology.detector.exceptions.CannottReadFromCache;
import org.ndx.aadarchi.technology.detector.helper.FileHelper;
import org.ndx.aadarchi.technology.detector.helper.InterestingArtifactsDetailsDownloader;
import org.ndx.aadarchi.technology.detector.loader.ExtractionContext;
import org.ndx.aadarchi.technology.detector.model.ArtifactDetails;
import org.ndx.aadarchi.technology.detector.model.GitHubDetails;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;

import io.github.emilyydev.asp.ProvidesService;

@ProvidesService(Augmenter.class)
public class AddGitHubStarsAtPeriod implements Augmenter {
	
	private static final Logger logger = Logger.getLogger(AddGitHubStarsAtPeriod.class.getName());
	private static String STARGAZERS_COUNT;
	private static String STARGAZERS_LIST;
	
	static {
		Properties properties = new Properties();
		try {
			try(InputStream input = AddGitHubStarsAtPeriod.class.getClassLoader().getResourceAsStream("github.xml")) {
				properties.loadFromXML(input);
			}
			STARGAZERS_LIST = properties.getProperty("stargazers_list");
			STARGAZERS_COUNT = properties.getProperty("stargazers_count");
		} catch(IOException e) {
			throw new Error("Cannot load github.xml file from CLASSPATH", e);
		}
	}
	
	@Override
	public int order() {
		return AddGitHub.ADD_GITHUB_OBJECT+10;
	}

	@Override
	public ArtifactDetails augment(ExtractionContext context, ArtifactDetails source, LocalDate date) {
		if(source.getGithubDetails()!=null) {
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
		String requestText = String.format(STARGAZERS_COUNT, githubDetails.getOwner(), githubDetails.getRepository());
		JsonNode response = runGraphQlRequest(githubToken, requestText);
		return response.get("data")
			.get("repository")
			.get("stargazerCount").asInt();
	}

	void extractStargazersHistory(ExtractionContext context, ArtifactDetails source, LocalDate date,
			GitHubDetails githubDetails) {
		Date old = Date.from(date.atStartOfDay(ZoneOffset.UTC).toInstant());
		List<Stargazer> allStargazers = getAllStargazers(context.getGithubToken(), context, source, githubDetails);
		long numberOfStargazersBefore = allStargazers.stream()
				.filter(s -> s.getStarredAt().compareTo(old)<0)
				.count();
		githubDetails.setStargazers(Optional.of((int) numberOfStargazersBefore));
	}

	List<Stargazer> getAllStargazers(String githubToken, ExtractionContext context, ArtifactDetails source, GitHubDetails details) {
		try {
			int total = getCurrentStargazersCount(githubToken, details);
			File cache = context.getCache()
					.resolve("github")
					.resolve(details.getPath())
					.resolve("stargazers.json")
					.toFile();
			cache.getParentFile().mkdirs();
			int percentage = 0;
			List<Stargazer> returned = null;
			if(cache.exists()) {
				returned = FileHelper.readFromFile(cache, new TypeReference<List<Stargazer>>() {});
				percentage = (int) (returned.size()/((float) total)*100);
			}
			if(percentage>0 && percentage<90) {
				logger.warning(
						String.format("Our cache doesn't contains enough data for %S (it contains only %d %%). Force refreshing",
								details.getPath(), percentage));
				cache.delete();
			}
			if(!cache.exists() || cache.lastModified()<System.currentTimeMillis()-Duration.ofDays(7).toMillis()) {
				List<Stargazer> stargazers = doGetAllStargazers(githubToken, details, total);
				try {
					FileHelper.writeToFile(stargazers, cache);
				} catch (IOException e) {
					throw new CannotWriteToCache("Can't write stargazers to "+cache.getAbsolutePath(), e);
				}
				returned = FileHelper.readFromFile(cache, new TypeReference<List<Stargazer>>() {});
			}
			return returned;
		} catch (IOException e) {
			throw new CannottReadFromCache("Can't read stargazers", e);
		}
	}

	List<Stargazer> doGetAllStargazers(String githubToken, GitHubDetails githubDetails, int total) {
		String repositoryOwner = githubDetails.getOwner();
		String repositoryName = githubDetails.getRepository();
		try {
			List<Stargazer> returned = new ArrayList<Stargazer>();
			String cursor = "";
			do {
				String requestText = String.format(STARGAZERS_LIST, repositoryOwner, repositoryName, cursor);
				JsonNode response = runGraphQlRequest(githubToken, requestText);
				cursor = getNextCursorValue(response);
				// Now process data
				JsonNode data = response.get("data");
				JsonNode repository = data.get("repository");
				JsonNode stargazers = repository.get("stargazers");
				StreamSupport.stream(stargazers.get("edges").spliterator(), false)
					.forEach(element -> {
						returned.add(toStargazer(element));
					});
			} while(cursor!=null && !cursor.equals(""));
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

	private JsonNode runGraphQlRequest(String githubToken, String requestText) {
		try {
			HttpRequest request = createGraphQLQuery(githubToken, requestText);
			HttpClient client = InterestingArtifactsDetailsDownloader.client;
			HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
			if(response.statusCode()<300) {
				return FileHelper.getObjectMapper().readTree(response.body());
			} else {
				throw new UnsupportedOperationException(
						String.format("Seems like HTTP request was incorrect (request was %s)(response was %s)",
								request.bodyPublisher().get(),
								response.body()));
			}
		} catch(IOException | InterruptedException e) {
			throw new CannotPerformGraphQL(
					String.format("Unable to perform graph query\n"
							+ "=====\n"
							+ "%s\n"
							+ "=====\n"
							+ "due to lower level exception", requestText), e);
		}
	}

	public static HttpRequest createGraphQLQuery(String githubToken, String query) {
		String jsonQuery = String.format("{ \"query\": \"%s\" }",
				// We have to embed query in JSON.
				// For that
				query
					// Quote line returns
					.replace("\n", "")
					// Quote double quotes
					.replace("\"", "\\\"")
				);
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create("https://api.github.com/graphql"))
				.header("Authorization", String.format("Bearer %s", githubToken))
				.header("Content-Type", "application/json; charset=utf-8")
				.POST(BodyPublishers.ofString(jsonQuery))
				.build();
		return request;
	}

}
