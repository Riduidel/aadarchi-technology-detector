package org.ndx.aadarchi.technology.detector.augmenters.github;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHStargazer;
import org.kohsuke.github.PagedIterable;
import org.ndx.aadarchi.technology.detector.augmenters.Augmenter;
import org.ndx.aadarchi.technology.detector.helper.FileHelper;
import org.ndx.aadarchi.technology.detector.loader.ExtractionContext;
import org.ndx.aadarchi.technology.detector.model.ArtifactDetails;
import org.ndx.aadarchi.technology.detector.model.ArtifactDetailsBuilder;

import com.fasterxml.jackson.core.type.TypeReference;

import io.github.emilyydev.asp.ProvidesService;

@ProvidesService(Augmenter.class)
public class AddGitHubStarsAtPeriod implements Augmenter {
	private static final Logger logger = Logger.getLogger(AddGitHubStarsAtPeriod.class.getName());
	
	public static final String GITHUB_REPOSITORIES = "github.repositories.properties";

	private final Properties githubProjects;

	{
		githubProjects = new Properties();
		try(InputStream input = AddGitHubStarsAtPeriod.class.getClassLoader().getResourceAsStream(GITHUB_REPOSITORIES)) {
			githubProjects.load(input);
		} catch (IOException e) {
			throw new RuntimeException("Can't read "+GITHUB_REPOSITORIES, e);
		}
	}

	@Override
	public ArtifactDetails augment(ExtractionContext context, ArtifactDetails source) {
		if(source.getUrls()!=null && source.getUrls().containsKey("github.com")) {
			return doAugment(context, source, source.getUrls().get("github.com").toString());
		} else if(githubProjects.containsKey(source.getCoordinates())) {
			return doAugment(context, source,githubProjects.getProperty(source.getCoordinates()));
		} else {
			logger.warning(String.format("There doesn't seems to be any github repo for "+source.getIdentifier()));
		}
		return source;
	}
	
	private ArtifactDetails doAugment(ExtractionContext context, ArtifactDetails source, String githubRepositoryUrl) {
		ArtifactDetailsBuilder builder = ArtifactDetailsBuilder.toBuilder(source);
		List<Stargazer> allStarGazers = getAllStargazers(context, source, githubRepositoryUrl);
		
		return builder.build();
	}

	private List<Stargazer> getAllStargazers(ExtractionContext context, ArtifactDetails source, String githubRepositoryUrl) {
		File cache = context.getCache()
				.resolve("github")
				.resolve(source.getIdentifier().replace(':', '-').replace('.', '-'))
				.resolve("stargazers.json")
				.toFile();
		cache.getParentFile().mkdirs();
		if(!cache.exists() || cache.lastModified()<System.currentTimeMillis()-Duration.ofDays(7).toMillis()) {
			List<Stargazer> stargazers = doGetAllStargazers(context, getGitHubPath(githubRepositoryUrl));
			try {
				FileHelper.writeToFile(stargazers, cache);
			} catch (IOException e) {
				throw new RuntimeException("Can't write stargazers to "+cache.getAbsolutePath(), e);
			}
		}
		try {
			return FileHelper.readFromFile(cache, new TypeReference<List<Stargazer>>() {});
		} catch (IOException e) {
			throw new RuntimeException("Can't read stargazers from "+cache.getAbsolutePath(), e);
		}
	}

	private String getGitHubPath(String githubRepositoryUrl) {
		String GITHUB = "github.com/";
		String returned = githubRepositoryUrl.substring(githubRepositoryUrl.indexOf(GITHUB)+GITHUB.length());
		return returned;
	}

	private List<Stargazer> doGetAllStargazers(ExtractionContext context, String githubRepositoryUrl) {
		try {
			logger.info("Fetching stargazers history of "+githubRepositoryUrl);
			GHRepository repository = context.getGithub().getRepository(githubRepositoryUrl);
			PagedIterable<GHStargazer> stargazers = repository
					.listStargazers2()
					.withPageSize(100);
			AtomicInteger atomic = new AtomicInteger(0);
			List<Stargazer> allStargazers = StreamSupport.stream(stargazers.spliterator(), false)
					.peek(consumer -> {
						int current = atomic.incrementAndGet();
						if(current%100==0) {
							logger.info(String.format("Fetched %d stargazers of %s", current, githubRepositoryUrl));
						}
					})
					.map(s -> new Stargazer(s))
					.sorted()
					.collect(Collectors.toList());
			return allStargazers;
		} catch (IOException e) {
			throw new RuntimeException("TODO handle IOException", e);
		} finally {
			logger.info("Fetched stargazers history of "+githubRepositoryUrl);
		}
	}

}
