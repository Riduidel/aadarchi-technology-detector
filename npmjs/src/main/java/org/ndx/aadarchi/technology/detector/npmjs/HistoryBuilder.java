package org.ndx.aadarchi.technology.detector.npmjs;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.AbortedByHookException;
import org.eclipse.jgit.api.errors.ConcurrentRefUpdateException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.NoMessageException;
import org.eclipse.jgit.api.errors.ServiceUnavailableException;
import org.eclipse.jgit.api.errors.UnmergedPathsException;
import org.eclipse.jgit.api.errors.WrongRepositoryStateException;
import org.eclipse.jgit.lib.PersonIdent;
import org.ndx.aadarchi.technology.detector.model.ArtifactDetails;

import com.github.fge.lambdas.Throwing;

class HistoryBuilder {

		private final Path gitHistory;
		private final Path cache;
		private final BiFunction<Collection<ArtifactDetails>, String, Collection<ArtifactDetails>> getAllDownloadsForPeriod;

		HistoryBuilder(Path gitHistory, Path cache, BiFunction<Collection<ArtifactDetails>, String, Collection<ArtifactDetails>> getAllDownloadsForPeriod) {
			this.gitHistory = gitHistory;
			this.cache = cache;
			this.getAllDownloadsForPeriod = getAllDownloadsForPeriod;
		}

		public static DateTimeFormatter DATE_FORMAT_WITH_DAY =
				new DateTimeFormatterBuilder()
					.appendPattern("MMM dd, yyyy")
					.parseCaseInsensitive()
					.toFormatter(Locale.ENGLISH)
					;

		public void generateHistoryFor(Collection<ArtifactDetails> allDetails) throws IOException {
			ExtractPopularNpmjsArtifacts.logger.info("Opening git repository at "+gitHistory.toFile().getAbsolutePath());
	    	Git git = Git.open(gitHistory.toFile());
	    	LocalDate initial = LocalDate.of(2010, 1, 1);
	    	ExtractPopularNpmjsArtifacts.logger.info("Fetching all dependencies since "+initial);
	    	Map<LocalDate, File> aggregatedStatuses = initial.datesUntil(LocalDate.now(), Period.ofMonths(1))
	    		.map(Throwing.function(month -> Map.entry(month, generateHistoryAtMonth(allDetails, month))))
	    		.filter(Throwing.predicate(entry -> {
	    			Collection<ArtifactDetails> infos = ExtractPopularNpmjsArtifacts.readArtifacts(entry.getValue());
	    			return !infos.isEmpty();
	    		}))
//	    		.forEach(entry -> logger.info("Got downloads at "+entry.getKey()))
	    		.collect(Collectors.toMap(Entry::getKey, Entry::getValue, (a, b) -> a, () -> new LinkedHashMap<LocalDate, File>()))
	    		;
	    	ExtractPopularNpmjsArtifacts.logger.info("Creating commits for all dates since "+initial);
		    
	    	// Write them into git history
	    	aggregatedStatuses.entrySet().stream()
	    		.forEach(Throwing.consumer(entry1 -> writeArtifactListAtDate(git, entry1.getKey(), entry1.getValue())));

		}
	    private void writeArtifactListAtDate(Git git, LocalDate date,
				File datedFilePath) throws IOException, AbortedByHookException, ConcurrentRefUpdateException, NoHeadException, NoMessageException, ServiceUnavailableException, UnmergedPathsException, WrongRepositoryStateException, GitAPIException {
	    	ExtractPopularNpmjsArtifacts.logger.info("Creating commit at "+date);
			Collection<ArtifactDetails> infos = ExtractPopularNpmjsArtifacts.readArtifacts(datedFilePath);
	    	File artifacts = new File(new File(gitHistory.toFile(), "npmjs"), "artifacts.json");
	    	FileUtils.copyFile(datedFilePath, artifacts);
	    	// Then create a commit in the history repository
	    	ZoneId systemZoneId = ZoneId.systemDefault();
			Instant commitInstant = date.atStartOfDay(systemZoneId).toInstant();
			PersonIdent commiter = new PersonIdent("🤖 Npmjs History Builder", 
					"get_npmjs_infos.yaml@history",
	    			commitInstant, systemZoneId);
			git.add()
				.addFilepattern("npmjs/artifacts.json")
				.call();
			git.commit()
	    		.setAuthor(commiter)
	    		.setCommitter(commiter)
	    		.setAll(true)
//	    		.setOnly("mvnrepository/artifacts.json")
//	    		.setAllowEmpty(false)
	    		.setMessage(String.format("Historical artifacts of %s, %d artifacts known at this date", 
	    				DATE_FORMAT_WITH_DAY.format(date), infos.size()))
	    		.call()
	    		;
		}
		private File getDatedFilePath(File containerDir,
				LocalDate timestamp) {
			return FileUtils.getFile(containerDir, 
					timestamp.getYear()+"", 
					timestamp.getMonthValue()+"",
					timestamp.getDayOfMonth()+"",
					"artifacts.json");
		}

		private File  generateHistoryAtMonth(Collection<ArtifactDetails> allDetails,
				LocalDate month) throws IOException {
			File destination = getDatedFilePath(new File(cache.toFile(), "captures"), month);
			if(!destination.exists()) {
				DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd");
				String monthlySearch = String.format("%s:%s",
						format.format(month),
						format.format(month.withDayOfMonth(month.getMonth().length(month.isLeapYear())))
						);
				Collection<ArtifactDetails> atMonth = getAllDownloadsForPeriod.apply(allDetails, monthlySearch);
		        ExtractPopularNpmjsArtifacts.writeArtifacts(atMonth, destination);

			}
			return destination;
		}
		
	}