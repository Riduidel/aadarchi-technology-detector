package org.ndx.aadarchi.technology.detector.mvnrepository;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
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
import org.ndx.aadarchi.technology.detector.model.Formats;

import com.github.fge.lambdas.Throwing;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;

class HistoryBuilder {
	public static final Logger logger = Logger.getLogger(ExtractPopularMvnRepositoryArtifacts.class.getName());

	HttpClient client = HttpClient.newHttpClient();
	
	private final File captures;
	private final File indices;
	private final File statuses;
	private final Path gitHistory;
	private final File artifactsLists;
	private final Path output;

	private Gson gson;

	private final String mvnRepositoryServer;

	private Function<BrowserContext, Page> newPage;

	private BiFunction<Page, String, Optional<Map>> addDetails;

	HistoryBuilder(String mvnRepositoryServer, Path gitHistory, Path cache, Path output, Gson gson, Function<BrowserContext, Page> newPage, BiFunction<Page, String, Optional<Map>> addDetails) {
		this.gitHistory = gitHistory;
		this.gson = gson;
		this.output = output;
		this.mvnRepositoryServer = mvnRepositoryServer;
		this.newPage = newPage;
		this.addDetails = addDetails;

		artifactsLists = new File(cache.toFile(), "artifacts");
		captures = new File(cache.toFile(), "captures");
		indices = new File(cache.toFile(), "indices");
		statuses = new File(cache.toFile(), "statuses"); 
	}

	/**
	 * Subverts all this class mechanism to generate a complete git history for all
	 * monthly statistics of all the given artifacts.
	 * 
	 * @param context
	 * @param allArtifactInformations
	 * @throws IOException
	 */
	void generateHistoryFor(BrowserContext context, List<ArtifactInformations> allArtifactInformations)
			throws IOException {
		Git git = Git.open(gitHistory.toFile());
		// First thing first, fill our cache with all remote infos we can have
		Map<ArtifactInformations, NavigableMap<LocalDate, File>> artifactsCaptures = allArtifactInformations.stream()
				.collect(Collectors.toMap(Function.identity(),
						Throwing.function(artifact -> cacheHistoryOf(context, artifact)),
						(firstMap, secondMap) -> firstMap));
		// Print some statistics
		logger.info("Processed artifacts\n" + artifactsCaptures.entrySet().stream()
				.map(e -> String.format("%s -> %d captures", e.getKey(), e.getValue().size()))
				.collect(Collectors.joining("\n")));
		List<ArtifactDetails> allArtifactsDetails = (List<ArtifactDetails>) gson
				.fromJson(
						FileUtils.readFileToString(output.toFile(), "UTF-8"),
						TypeToken.getParameterized(List.class, ArtifactDetails.class));
		// Move history details from the future to the past for the whole timeline
		artifactsCaptures.entrySet()
				.forEach(entry -> copyHistoryFromLastToFirst(entry.getKey(), allArtifactsDetails, entry.getValue()));
		Collection<ArtifactDetails> currentArtifacts = gson.fromJson(
				FileUtils.readFileToString(output.toFile(), "UTF-8"),
				new TypeToken<Collection<ArtifactDetails>>() {
				});
		// Generate complete artifact informations through inference
		Map<ArtifactInformations, NavigableMap<LocalDate, File>> artifactsStatuses = artifactsCaptures.entrySet()
				.stream().collect(Collectors.toMap(entry -> entry.getKey(), Throwing.function(
						entry -> generateAllStatusesFrom(entry.getKey(), entry.getValue(), currentArtifacts))));
		// Create one artifacts.json per month
		var aggregatedStatuses = aggregateStatusesPerDate(artifactsStatuses);
		// Write them into git history
		aggregatedStatuses.entrySet().stream()
				.forEach(Throwing.consumer(entry1 -> writeArtifactListAtDate(git, entry1.getKey(), entry1.getValue())));
	}

	private void writeArtifactListAtDate(Git git, LocalDate date, SortedSet<ArtifactDetails> value) throws IOException,
			AbortedByHookException, ConcurrentRefUpdateException, NoHeadException, NoMessageException,
			ServiceUnavailableException, UnmergedPathsException, WrongRepositoryStateException, GitAPIException {
		// First, write the artifact in its own folder
		File datedFilePath = getDatedFilePath(artifactsLists, date,
				"artifacts");
		logger.info(String.format("Writing %d artifacts of %s into %s",
				value.size(), Formats.MVN_DATE_FORMAT_WITH_DAY.format(date), datedFilePath));
		FileUtils.write(datedFilePath, gson.toJson(value), "UTF-8");
		File artifacts = new File(
				new File(gitHistory.toFile(), "mvnrepository"),
				"artifacts.json");
		FileUtils.copyFile(datedFilePath, artifacts);
		// Then create a commit in the history repository
		ZoneId systemZoneId = ZoneId.systemDefault();
		Instant commitInstant = date.atStartOfDay(systemZoneId).toInstant();
		PersonIdent commiter = new PersonIdent("🤖 MvnRepository History Builder",
				"get_mvnrepository_infos.yaml@history", commitInstant, systemZoneId);
		git.add().addFilepattern("resources/mvnrepository/artifacts.json").call();
		git.commit().setAuthor(commiter).setCommitter(commiter).setAll(true)
//	    		.setOnly("mvnrepository/artifacts.json")
//	    		.setAllowEmpty(false)
				.setMessage(String.format("Historical artifacts of %s, %d artifacts known at this date",
						Formats.MVN_DATE_FORMAT_WITH_DAY.format(date), value.size()))
				.call();
	}

	/**
	 * Aggregate statuses per month, write the artifacts.json files and put them
	 * into a per-date map (for creating the git commits)
	 * 
	 * @param artifactsStatuses
	 * @return
	 * @throws IOException
	 * @throws JsonSyntaxException
	 */
	private Map<LocalDate, SortedSet<ArtifactDetails>> aggregateStatusesPerDate(
			Map<ArtifactInformations, NavigableMap<LocalDate, File>> artifactsStatuses)
			throws JsonSyntaxException, IOException {
		// We directly take values, because they should all already be first of month
		// ones
		Map<LocalDate, SortedSet<ArtifactDetails>> artifactLists = new TreeMap<>();
		for (Map<LocalDate, File> map : artifactsStatuses.values()) {
			for (Entry<LocalDate, File> entry : map.entrySet()) {
				LocalDate key = entry.getKey();
				if (!artifactLists.containsKey(key)) {
					artifactLists.put(key, new TreeSet<>());
				}
				artifactLists.get(key).add(gson
						.fromJson(FileUtils.readFileToString(entry.getValue(), "UTF-8"), ArtifactDetails.class));
			}
		}
		return artifactLists;
	}

	private void copyHistoryFromLastToFirst(ArtifactInformations key, List<ArtifactDetails> allArtifactInformations,
			NavigableMap<LocalDate, File> value) {
		Optional<ArtifactDetails> associatedArtifactDetail = allArtifactInformations.stream()
				.filter(artifact -> artifact.coordinates.equals(key.toCoordinates())).findFirst();
		if (associatedArtifactDetail.isPresent()) {
			copyHistoryFromLastToFirst(key, associatedArtifactDetail.get(), value);
		} else {
			logger
					.warning(String.format("Unable to copy history from missing last artifact %s", key));
		}
	}

	/**
	 * Copy fine release date from the last element to the first in the whole
	 * artifact history. Because MvnRepository contains release dates only for
	 * recent versions
	 * 
	 * @param artifact
	 * @param capturesHistory
	 */
	private void copyHistoryFromLastToFirst(ArtifactInformations artifact, ArtifactDetails details,
			NavigableMap<LocalDate, File> capturesHistory) {
		ArtifactDetails newDetails = null;
		capturesHistory.forEach((date, file) -> copyDatesInto(details, date, file));
	}

	/**
	 * Copy the versions date from the details object into the oldDetails object
	 * (which will obviously be changed)
	 */
	private void copyDatesInto(ArtifactDetails details, LocalDate date, File file) {
		try {
			ArtifactDetails oldDetails = gson
					.fromJson(FileUtils.readFileToString(file, "UTF-8"), ArtifactDetails.class);
			Optional<ArtifactDetails> changedDetailsOptiona = oldDetails.copyDatesFrom(details);
			if (changedDetailsOptiona.isPresent()) {
				ArtifactDetails changedDetails = changedDetailsOptiona.get();
				FileUtils.write(file, gson.toJson(changedDetails), "UTF-8");
				oldDetails = changedDetails;
			}
		} catch (IOException e) {
			throw new UnsupportedOperationException("Unable to process some file", e);
		}
	}

	/**
	 * Generate a list of status for each month (since first known version) for the
	 * given artifact. For that, usage will be infered through linear regression
	 * between the various most recent points. When no usage can be found, well, we
	 * imagine something
	 * 
	 * @param artifact
	 * @param knownHistory
	 * @return
	 * @throws IOException
	 * @throws JsonSyntaxException
	 */
	public NavigableMap<LocalDate, File> generateAllStatusesFrom(ArtifactInformations artifact,
			NavigableMap<LocalDate, File> knownHistory, Collection<ArtifactDetails> currentArtifacts)
			throws JsonSyntaxException, IOException {
		logger
				.info(String.format("generating all statuses for %s", artifact.toCoordinates()));
		NavigableMap<LocalDate, File> returned = new TreeMap<LocalDate, File>();
		LocalDate now = LocalDate.now();
		LocalDate currentMonth = LocalDate.of(now.getYear(), now.getMonthValue(), 1);
		LocalDate startMonth = LocalDate.of(2006, 1, 1);
		while (currentMonth.compareTo(startMonth) > 0) {
			Optional<File> capture = generateStatusFor(currentMonth, artifact, knownHistory, currentArtifacts);
			if (capture.isPresent()) {
				returned.put(currentMonth, capture.get());
			}
			currentMonth = currentMonth.minusMonths(1);
		}
		return returned;
	}

	private Optional<File> generateStatusFor(LocalDate currentMonth, ArtifactInformations artifact,
			NavigableMap<LocalDate, File> knownHistory, Collection<ArtifactDetails> currentArtifacts)
			throws JsonSyntaxException, IOException {
		File output = getDatedFilePath(statuses, currentMonth,
				artifact.toCoordinates());
		if (output.exists())
			return Optional.of(output);
		Entry<LocalDate, File> fileSnapshotBefore = knownHistory.floorEntry(currentMonth);
		Entry<LocalDate, File> fileSnapshotAfter = knownHistory.ceilingEntry(currentMonth);
		ArtifactDetails dataBefore = null;
		ArtifactDetails dataAfter = null;
		Optional<ArtifactDetails> currentData = currentArtifacts.stream()
				.filter(a -> a.coordinates.equals(artifact.toCoordinates())).findFirst();
		if (currentData.isEmpty())
			return Optional.empty();
		if (fileSnapshotBefore != null) {
			if (fileSnapshotBefore.getKey().compareTo(currentMonth) < 0) {
				dataBefore = gson.fromJson(
						FileUtils.readFileToString(fileSnapshotBefore.getValue(), "UTF-8"), ArtifactDetails.class);
			} else {
				fileSnapshotBefore = null;
			}
		}
		LocalDate dateAfter = null;
		if (fileSnapshotAfter == null) {
			dateAfter = currentMonth;
			dataAfter = currentData.get();
		} else {
			dataAfter = gson
					.fromJson(FileUtils.readFileToString(fileSnapshotAfter.getValue(), "UTF-8"), ArtifactDetails.class);
			dateAfter = fileSnapshotAfter.getKey();
		}
		if (fileSnapshotBefore == null)
			return Optional.empty();
		// First step is simple: fill missing data
		if (dataAfter != null) {
			ArtifactDetails currentMonthData = fileSnapshotBefore == null
					? dataAfter.inferDataPoint(currentData.get(), dateAfter, currentMonth, null, null)
					: dataAfter.inferDataPoint(currentData.get(), dateAfter, currentMonth, fileSnapshotBefore.getKey(),
							dataBefore);
			// Now we have an artifact, write it to disk
			FileUtils.write(output, gson.toJson(currentMonthData), "UTF-8");
			return Optional.of(output);
		}
		return Optional.empty();
	}

	/**
	 * Obtain the artifact history and cache it locally
	 * 
	 * @param context
	 * @param artifact
	 * @return true if all entries have been processed successfully (files are
	 *         present on machine for each entry)
	 * @throws URISyntaxException
	 * @throws InterruptedException
	 * @throws IOException
	 */
	private NavigableMap<LocalDate, File> cacheHistoryOf(BrowserContext context, ArtifactInformations artifact)
			throws IOException, InterruptedException, URISyntaxException {
		var history = getArtifactHistoryOnCDX(artifact);
		return history.stream().map(archivePoint -> obtainArchivePointFor(artifact, archivePoint, context))
				.filter(entry -> entry != null).filter(entry -> entry.getValue() != null)
				.collect(Collectors.toMap(entry -> entry.getKey().toLocalDate(), entry -> entry.getValue(), (a, b) -> a,
						() -> new TreeMap<>()));
	}

	private Entry<LocalDateTime, File> obtainArchivePointFor(ArtifactInformations artifact, ArchivePoint archivePoint,
			BrowserContext context) {
		File destination = getDatedFilePath(captures,
				archivePoint.timestamp().toLocalDate(), artifact.toCoordinates());
		if (destination.exists()) {
			if (destination.length() < 200) {
				logger
						.warning(String.format("Seems like artifact %s has no content. Removing it.", archivePoint));
				destination.delete();
			} else {
				return Map.entry(archivePoint.timestamp(), destination);
			}
		}
		var url = String.format("https://web.archive.org/web/%s/%s",
				Formats.INTERNET_ARCHIVE_DATE_FORMAT.format(archivePoint.timestamp()),
				artifact.getArtifactUrl(mvnRepositoryServer));
		try (Page page = newPage.apply(context)) {

			var details = addDetails.apply(page, url);
			details.ifPresent(Throwing.consumer(map -> {
				var json = gson.toJson(map);
				FileUtils.write(destination, json, "UTF-8");
			}));
			return Map.entry(archivePoint.timestamp(), destination);
		} catch (PlaywrightException e) {
			logger.log(Level.WARNING,
					String.format("Unable to add cache entry %s due to %s", archivePoint, e.getMessage()));
		}
		return null;
	}

	private File getDatedFilePath(File containerDir, LocalDate timestamp, String name) {
		return FileUtils.getFile(containerDir, timestamp.getYear() + "", timestamp.getMonthValue() + "",
				timestamp.getDayOfMonth() + "", name + ".json");
	}

	private List<ArchivePoint> getArtifactHistoryOnCDX(ArtifactInformations artifact)
			throws IOException, InterruptedException, URISyntaxException {
		File cache = new File(indices, artifact.toCoordinates() + ".json");
		String url = String.format("http://web.archive.org/cdx/search/cdx" + "?filter=statuscode:200" // We take only
																										// valid
																										// archive.org
																										// captures
				+ "&output=json" + "&collapse=timestamp:6" // We look at the month scale
				+ "&url=%s", artifact.getArtifactUrl(mvnRepositoryServer));
		if (!cache.exists()) {
			// Run request
			try {
				HttpResponse<String> response = client.send(HttpRequest.newBuilder(new URI(url)).build(),
						BodyHandlers.ofString());
				if (response.statusCode() < 300) {
					String text = response.body();
					FileUtils.write(cache, text, "UTF-8");
				} else {
					throw new UnsupportedOperationException(String
							.format("Can't get content from webarchive with status code %d", response.statusCode()));
				}
			} catch (IOException e) {
				logger.log(Level.WARNING,
						String.format("Couldn't get history for %s due to %s", artifact, e.getMessage()));
				return Arrays.asList();
			}
		}
		String text = FileUtils.readFileToString(cache, "UTF-8");
		return gson.fromJson(text, new TypeToken<List<List<String>>>() {
		}).stream().filter(row -> !row.get(0).equals("urlkey"))
				.map(Throwing.function(row -> new ArchivePoint(row.get(0),
						LocalDateTime.parse(row.get(1), Formats.INTERNET_ARCHIVE_DATE_FORMAT), row.get(2), row.get(5),
						row.get(6))))
				.sorted(Comparator.comparing(ArchivePoint::timestamp)).collect(Collectors.toList());
	}
}