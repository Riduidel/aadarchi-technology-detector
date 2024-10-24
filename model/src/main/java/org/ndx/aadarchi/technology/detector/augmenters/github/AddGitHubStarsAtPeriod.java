package org.ndx.aadarchi.technology.detector.augmenters.github;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.kohsuke.github.GHFileNotFoundException;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHStargazer;
import org.kohsuke.github.PagedIterable;
import org.ndx.aadarchi.technology.detector.augmenters.Augmenter;
import org.ndx.aadarchi.technology.detector.exceptions.CannottReadFromCache;
import org.ndx.aadarchi.technology.detector.exceptions.CannotWriteToCache;
import org.ndx.aadarchi.technology.detector.helper.FileHelper;
import org.ndx.aadarchi.technology.detector.loader.ExtractionContext;
import org.ndx.aadarchi.technology.detector.model.ArtifactDetails;
import org.ndx.aadarchi.technology.detector.model.GitHubDetails;

import com.fasterxml.jackson.core.type.TypeReference;

import io.github.emilyydev.asp.ProvidesService;

@ProvidesService(Augmenter.class)
public class AddGitHubStarsAtPeriod implements Augmenter {
	private static final Logger logger = Logger.getLogger(AddGitHubStarsAtPeriod.class.getName());
	
	@Override
	public int order() {
		return AddGitHub.ADD_GITHUB_OBJECT+1;
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
		try {
			GHRepository repository = context.getGithub().getRepository(source.getGithubDetails().getPath());
			githubDetails.setStargazers(Optional.of(repository.getStargazersCount()));
		} catch (IOException e) {
			logger.log(Level.WARNING, String.format("Can't get stargazers count for artifact %s (supposedly at %s)", source.getCoordinates(), githubDetails.getPath()), e);
		}
	}

	private void extractStargazersHistory(ExtractionContext context, ArtifactDetails source, LocalDate date,
			GitHubDetails githubDetails) {
		Date old = Date.from(date.atStartOfDay(ZoneOffset.UTC).toInstant());
		List<Stargazer> allStargazers = getAllStargazers(context, source, githubDetails);
		long numberOfStargazersBefore = allStargazers.stream()
				.filter(s -> s.getStarredAt().compareTo(old)<0)
				.count();
		githubDetails.setStargazers(Optional.of((int) numberOfStargazersBefore));
	}

	private List<Stargazer> getAllStargazers(ExtractionContext context, ArtifactDetails source, GitHubDetails details) {
		try {
			GHRepository repository = context.getGithub().getRepository(details.getPath());
			int total = repository.getStargazersCount();
			File cache = context.getCache()
					.resolve("github")
					.resolve(details.getPath())
					.resolve("stargazers.json")
					.toFile();
			cache.getParentFile().mkdirs();
			List<Stargazer> returned = FileHelper.readFromFile(cache, new TypeReference<List<Stargazer>>() {});
			int percentage = (int) (returned.size()/((float) total)*100);
			if(percentage>0 && percentage<90) {
				logger.warning(
						String.format("Our cache doesn't contains enough data for %S (it contains only %d %%). Force refreshing",
								details.getPath(), percentage));
				cache.delete();
			}
			if(!cache.exists() || cache.lastModified()<System.currentTimeMillis()-Duration.ofDays(7).toMillis()) {
				List<Stargazer> stargazers = doGetAllStargazers(repository, total);
				try {
					FileHelper.writeToFile(stargazers, cache);
				} catch (IOException e) {
					throw new CannotWriteToCache("Can't write stargazers to "+cache.getAbsolutePath(), e);
				}
				returned = FileHelper.readFromFile(cache, new TypeReference<List<Stargazer>>() {});
			}
			return returned;
		} catch (GHFileNotFoundException e) {
			logger.log(Level.SEVERE, "Weirdly, repository "+details.getPath()+" doesn't seems to exist");
			return Collections.emptyList();
		} catch (IOException e) {
			throw new CannottReadFromCache("Can't read stargazers", e);
		}
	}

	private List<Stargazer> doGetAllStargazers(GHRepository repository, int total) {
		try {
			logger.info("Fetching stargazers history of "+repository.getFullName());
			PagedIterable<GHStargazer> stargazers = repository
					.listStargazers2()
					.withPageSize(100);
			AtomicInteger atomic = new AtomicInteger(0);
			List<Stargazer> allStargazers = StreamSupport.stream(stargazers.spliterator(), false)
					.peek(consumer -> {
						int current = atomic.incrementAndGet();
						if(current%100==0) {
							logger.info(String.format("Fetched %d/%d stargazers of %s", 
									current, total, repository.getFullName()));
						}
					})
					.map(s -> new Stargazer(s))
					.sorted()
					.collect(Collectors.toList());
			return allStargazers;
		} finally {
			logger.info("Fetched stargazers history of "+repository.getFullName());
		}
	}

}
