package org.ndx.aadarchi.technology.detector.mvnrepository;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.ndx.aadarchi.technology.detector.helper.FileHelper;
import org.ndx.aadarchi.technology.detector.helper.InterestingArtifactsDetailsDownloader;
import org.ndx.aadarchi.technology.detector.history.BaseHistoryBuilder;
import org.ndx.aadarchi.technology.detector.model.ArtifactDetails;
import org.ndx.aadarchi.technology.detector.model.Formats;

import com.github.fge.lambdas.Throwing;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;

/**
 * As I dodn't found any artifact download history for maven artifact, I had to resign and use the internet wayback machine.
 * As a consequence, figures here are infered, rather than cleanly obtained.
 */
public class HistoryBuilder extends BaseHistoryBuilder<MvnContext> {
	public static final Logger logger = Logger.getLogger(ExtractPopularMvnRepositoryArtifacts.class.getName());

	private final File captures;
	private final File indices;
	private final File statuses;
	public final File artifactsLists;
	private final Path output;

	private final String mvnRepositoryServer;

	HistoryBuilder(String mvnRepositoryServer, Path gitHistory, Path cache, Path output) {
		super(cache, "🤖 MvnRepository History Builder", 
				"get_mvnrepository_infos.yaml@history",
				"mvnrepository");
		this.output = output;
		this.mvnRepositoryServer = mvnRepositoryServer;

		artifactsLists = new File(cache.toFile(), "artifacts");
		captures = new File(cache.toFile(), "captures");
		indices = new File(cache.toFile(), "indices");
		statuses = new File(cache.toFile(), "statuses"); 
	}

	@Override
	protected
	SortedMap<LocalDate, File> generateAggregatedStatusesMap(MvnContext context,
			Collection<ArtifactDetails> allArtifactInformations) throws IOException {
		// First thing first, fill our cache with all remote infos we can have
		Map<ArtifactDetails, NavigableMap<LocalDate, File>> artifactsCaptures = allArtifactInformations.stream()
				.collect(Collectors.toMap(Function.identity(),
						Throwing.function(artifact -> cacheHistoryOf(context, artifact)),
						(firstMap, secondMap) -> firstMap));
		// Print some statistics
		logger.info("Processed artifacts\n" + artifactsCaptures.entrySet().stream()
				.map(e -> String.format("%s -> %d captures", e.getKey(), e.getValue().size()))
				.collect(Collectors.joining("\n")));
		List<ArtifactDetails> allArtifactsDetails = (List<ArtifactDetails>) FileHelper.gson
				.fromJson(
						FileUtils.readFileToString(output.toFile(), "UTF-8"),
						TypeToken.getParameterized(List.class, ArtifactDetails.class));
		// Move history details from the future to the past for the whole timeline
		artifactsCaptures.entrySet()
				.forEach(entry -> copyHistoryFromLastToFirst(entry.getKey(), allArtifactsDetails, entry.getValue()));
		Collection<ArtifactDetails> currentArtifacts = FileHelper.gson.fromJson(
				FileUtils.readFileToString(output.toFile(), "UTF-8"),
				new TypeToken<Collection<ArtifactDetails>>() {
				});
		// Generate complete artifact informations through inference
		Map<ArtifactDetails , NavigableMap<LocalDate, File>> artifactsStatuses = artifactsCaptures.entrySet()
				.stream().collect(Collectors.toMap(entry -> entry.getKey(), Throwing.function(
						entry -> generateAllStatusesFrom(entry.getKey(), entry.getValue(), currentArtifacts))));
		// Create one artifacts.json per month
		SortedMap<LocalDate, File> aggregatedStatuses = aggregateStatusesPerDate(artifactsStatuses);
		return aggregatedStatuses;
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
	private SortedMap<LocalDate, File> aggregateStatusesPerDate(
			Map<ArtifactDetails, NavigableMap<LocalDate, File>> artifactsStatuses)
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
				artifactLists.get(key).add(FileHelper.gson
						.fromJson(FileUtils.readFileToString(entry.getValue(), "UTF-8"), ArtifactDetails.class));
			}
		}
		// Now write all those artifact lists to files
		SortedMap<LocalDate, File> datesToFiles = artifactLists.entrySet().stream()
			.map(Throwing.function(entry -> {
				// First, write the artifact in its own folder
				File datedFilePath = getDatedFilePath(artifactsLists, entry.getKey(),
						"artifacts");
				logger.info(String.format("Writing %d artifacts of %s into %s",
						entry.getValue().size(), Formats.MVN_DATE_FORMAT_WITH_DAY.format(entry.getKey()), datedFilePath));
				FileUtils.write(datedFilePath, FileHelper.gson.toJson(entry.getValue()), "UTF-8");
				return Map.entry(entry.getKey(), datedFilePath);
			}))
			.collect(Collectors.toMap(Entry::getKey, Entry::getValue, (a, b) -> a, () -> new TreeMap<LocalDate, File>()));
		return datesToFiles;
	}

	private void copyHistoryFromLastToFirst(ArtifactDetails key, List<ArtifactDetails> allArtifactInformations,
			NavigableMap<LocalDate, File> value) {
		Optional<ArtifactDetails> associatedArtifactDetail = allArtifactInformations.stream()
				.filter(artifact -> artifact.getCoordinates().equals(key.getCoordinates())).findFirst();
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
	private void copyHistoryFromLastToFirst(ArtifactDetails artifact, ArtifactDetails details,
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
			ArtifactDetails oldDetails = FileHelper.gson
					.fromJson(FileUtils.readFileToString(file, "UTF-8"), ArtifactDetails.class);
			Optional<ArtifactDetails> changedDetailsOptiona = oldDetails.copyDatesFrom(details);
			if (changedDetailsOptiona.isPresent()) {
				ArtifactDetails changedDetails = changedDetailsOptiona.get();
				FileUtils.write(file, FileHelper.gson.toJson(changedDetails), "UTF-8");
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
	public NavigableMap<LocalDate, File> generateAllStatusesFrom(ArtifactDetails artifact,
			NavigableMap<LocalDate, File> knownHistory, Collection<ArtifactDetails> currentArtifacts)
			throws JsonSyntaxException, IOException {
		logger
				.info(String.format("generating all statuses for %s", artifact.getCoordinates()));
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

	private Optional<File> generateStatusFor(LocalDate currentMonth, ArtifactDetails artifact,
			NavigableMap<LocalDate, File> knownHistory, Collection<ArtifactDetails> currentArtifacts)
			throws JsonSyntaxException, IOException {
		File output = getDatedFilePath(statuses, currentMonth,
				artifact.getCoordinates());
		if (output.exists())
			return Optional.of(output);
		Entry<LocalDate, File> fileSnapshotBefore = knownHistory.floorEntry(currentMonth);
		Entry<LocalDate, File> fileSnapshotAfter = knownHistory.ceilingEntry(currentMonth);
		ArtifactDetails dataBefore = null;
		ArtifactDetails dataAfter = null;
		Optional<ArtifactDetails> currentData = currentArtifacts.stream()
				.filter(a -> a.getCoordinates().equals(artifact.getCoordinates())).findFirst();
		if (currentData.isEmpty())
			return Optional.empty();
		if (fileSnapshotBefore != null) {
			if (fileSnapshotBefore.getKey().compareTo(currentMonth) < 0) {
				dataBefore = FileHelper.gson.fromJson(
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
			dataAfter = FileHelper.gson
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
			FileUtils.write(output, FileHelper.gson.toJson(currentMonthData), "UTF-8");
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
	private NavigableMap<LocalDate, File> cacheHistoryOf(MvnContext context, ArtifactDetails artifact)
			throws IOException, InterruptedException, URISyntaxException {
		var history = getArtifactHistoryOnCDX(artifact);
		return history.stream().map(archivePoint -> obtainArchivePointFor(artifact, archivePoint, context))
				.filter(entry -> entry != null).filter(entry -> entry.getValue() != null)
				.collect(Collectors.toMap(entry -> entry.getKey().toLocalDate(), entry -> entry.getValue(), (a, b) -> a,
						() -> new TreeMap<>()));
	}

	private Entry<LocalDateTime, File> obtainArchivePointFor(ArtifactDetails artifact, ArchivePoint archivePoint,
			MvnContext context) {
		File destination = getDatedFilePath(captures,
				archivePoint.timestamp().toLocalDate(), artifact.getCoordinates());
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
				MvnContext.getArtifactUrl(artifact, mvnRepositoryServer));
		var details = context.addDetails(url);
		details.ifPresent(Throwing.consumer(map -> {
			var json = FileHelper.gson.toJson(map);
			FileUtils.write(destination, json, "UTF-8");
		}));
		return Map.entry(archivePoint.timestamp(), destination);
	}

	public File getDatedFilePath(File containerDir, LocalDate timestamp, String name) {
		return FileUtils.getFile(containerDir, timestamp.getYear() + "", timestamp.getMonthValue() + "",
				timestamp.getDayOfMonth() + "", name + ".json");
	}
	private List<ArchivePoint> getArtifactHistoryOnCDX(ArtifactDetails artifact)
			throws IOException, InterruptedException, URISyntaxException {
		File cache = new File(indices, artifact.getCoordinates() + ".json");
		String url = String.format("http://web.archive.org/cdx/search/cdx" + "?filter=statuscode:200" // We take only
																										// valid
																										// archive.org
																										// captures
				+ "&output=json" + "&collapse=timestamp:6" // We look at the month scale
				+ "&url=%s", MvnContext.getArtifactUrl(artifact, mvnRepositoryServer));
		if (!cache.exists()) {
			// Run request
			try {
				HttpResponse<String> response = InterestingArtifactsDetailsDownloader.client.send(HttpRequest.newBuilder(new URI(url)).build(),
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
		return FileHelper.gson.fromJson(text, new TypeToken<List<List<String>>>() {
		}).stream().filter(row -> !row.get(0).equals("urlkey"))
				.map(Throwing.function(row -> new ArchivePoint(row.get(0),
						LocalDateTime.parse(row.get(1), Formats.INTERNET_ARCHIVE_DATE_FORMAT), row.get(2), row.get(5),
						row.get(6))))
				.sorted(Comparator.comparing(ArchivePoint::timestamp)).collect(Collectors.toList());
	}
}