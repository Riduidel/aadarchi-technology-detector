package org.ndx.aadarchi.technology.detector.pypi;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.ndx.aadarchi.technology.detector.helper.FileHelper;
import org.ndx.aadarchi.technology.detector.history.BaseHistoryBuilder;
import org.ndx.aadarchi.technology.detector.model.ArtifactDetails;
import org.ndx.aadarchi.technology.detector.model.ArtifactDetailsBuilder;

import com.github.fge.lambdas.Throwing;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.Job;
import com.google.cloud.bigquery.JobInfo;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.cloud.bigquery.TableResult;
import org.ndx.aadarchi.technology.detector.pypi.exception.CannotExecuteBigQuery;
import org.ndx.aadarchi.technology.detector.pypi.exception.CannotWriteBigQueryResultsToFile;
import org.ndx.aadarchi.technology.detector.pypi.exception.CannotLoadBigQueryFromProperties;

public class HistoryBuilder extends BaseHistoryBuilder<PypiContext> {
	public static final Logger logger = Logger.getLogger(HistoryBuilder.class.getName());
	/**
	 * Query to run on BigQuery to get all results
	 */
	private final String query;
	/**
	 * GCP BigQuery project (for accounting)
	 */
	private final String projectId;
	private File csvResultsFile;
	private boolean reduced;
	private File historyBaseFolder;
	DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("uuuu-MM-dd");

	public HistoryBuilder(Path gitHistory, Path cache, String projectId, String queryName, boolean forceRebuildHistory) {
		super(cache, "🤖 Pypi History Builder", "get_pypi_infos.yaml@history", "pypi", forceRebuildHistory);
		this.projectId = projectId;
		query = loadQuery(queryName);
		csvResultsFile = new File("src/main/resources/big_query_results/"+queryName+".csv");
		historyBaseFolder = new File(cache.toFile(), "history");
	}

	/**
	 * Load given BigQuery code from query name
	 * @param queryName query to load
	 * @return the content of the `query.xml` file named by the input parameter
	 */
	private String loadQuery(String queryName) {
		Properties properties = new Properties();
		try {
			try (InputStream queryStream = getClass().getClassLoader().getResourceAsStream("query.xml")) {
				properties.loadFromXML(queryStream);
				return properties.getProperty(queryName);
			}
		} catch (IOException e) {
			throw new CannotLoadBigQueryFromProperties("Unable to open query.xml", e);
		}
	}

	@Override
	protected SortedMap<LocalDate, File> generateAggregatedStatusesMap(PypiContext context,
			Collection<ArtifactDetails> allArtifactInformations) throws IOException {
		if(reduced) {
			logger.info("Full list of packages is "+
					allArtifactInformations.stream()
					.map(ArtifactDetails::getName)
					.map(n -> "'"+n+"'")
					.collect(Collectors.toList()));
			allArtifactInformations = allArtifactInformations.stream()
					.limit(3)
					.collect(Collectors.toList());
			logger.info("Searching for popularity history of SOME packages");
			return runBigQuery(allArtifactInformations);
		} else {
			logger.info("Searching for popularity history of all packages");
			return runBigQuery(allArtifactInformations);
		}
	}

	/**
	 * Yeah runBigQuery NEVER runs bigquery, but rather gets the results from a BigQuery run by Zenika
	 * @param allArtifactInformations
	 * @return
	 */
	private SortedMap<LocalDate, File> runBigQuery(Collection<ArtifactDetails> allArtifactInformations) {
		List<String> names = allArtifactInformations.stream().map(ArtifactDetails::getName)
				.collect(Collectors.toList());
		Collection<Map.Entry<LocalDate, Map.Entry<String, Long>>> basicallyParsed = readBigQueryPackageDownloadsAsCSVResult(names);
		return processResults(allArtifactInformations, basicallyParsed);
	}
	
	private SortedMap<LocalDate, File> processResults(Collection<ArtifactDetails> allArtifactInformations, Collection<Map.Entry<LocalDate, Map.Entry<String, Long>>> result) {
		Map<LocalDate, Map<String, Long>> groupedByDate = 
				StreamSupport.stream(result.spliterator(), false)
			.collect(Collectors.groupingBy(e -> e.getKey(),
					Collectors.toMap(e -> e.getValue().getKey(), e -> e.getValue().getValue())
					));
		return groupedByDate.entrySet().stream()
			.map(Throwing.function(entry -> writeDownloadsAtDate(allArtifactInformations, entry.getKey(), entry.getValue())))
			.collect(
					Collectors.toMap(
							e -> e.getKey(), 
							e -> e.getValue(),
							(a, b) -> a,
							() -> new TreeMap<LocalDate, File>()))
			;
	}

	private Map.Entry<LocalDate, File> writeDownloadsAtDate(Collection<ArtifactDetails> allArtifacts, LocalDate month,
			Map<String, Long> value) throws IOException {
		File artifactsAtDateFile = new File(historyBaseFolder,
				String.format("%s/%s/%s/artifacts.json", 
						month.getYear(), month.getMonthValue(), month.getDayOfMonth())
				);
		List<ArtifactDetails> artifactsAtDateCollection = allArtifacts.stream()
				.map(artifact -> ArtifactDetailsBuilder.toBuilder(artifact)
						.downloads(value.getOrDefault(artifact.getName(), -1l))
						.build())
				.collect(Collectors.toList());
		logger.info("Writing artifacts for "+month.getYear()+"/"+month.getMonthValue());
		FileHelper.writeToFile(artifactsAtDateCollection, artifactsAtDateFile);
		return Map.entry(month, artifactsAtDateFile);
	}

	private Map.Entry<LocalDate, Map.Entry<String, Long>> recordToEntries(CSVRecord r) {
		LocalDate month = LocalDate.from(DateTimeFormatter.ISO_LOCAL_DATE.parse(r.get("month")));
		String project = r.get("project");
		long downloads = Long.parseLong(r.get("number_of_downloads"));
		return Map.entry(month, Map.entry(project, downloads));
	}

	/**
	 * Transform CSV results of download counts into a specifc type
	 * @param <Type>
	 * @param names
	 * @param function
	 * @return
	 */
	private Collection<Map.Entry<LocalDate, Map.Entry<String, Long>>> readBigQueryPackageDownloadsAsCSVResult(List<String> names) {
		CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
				.setHeader("month", "project", "number_of_downloads")
				.setSkipHeaderRecord(true)
				.build();
		if(!csvResultsFile.exists()) {
			// We ignore results older that 2016, since it's roughly at this date that
			// Pypi changed its statistics gathering mechanism
			writeTableResultToCSV(csvFormat, names, LocalDate.of(2016, 1, 1), csvResultsFile);
		}
		return readBigQueryPackagesDownloads(csvFormat, names, csvResultsFile);
	}

	/**
	 * Read all packages download as stream.
	 * @param csvFormat
	 * @param names 
	 * @param csvFile TODO
	 * @return
	 */
	private Collection<Map.Entry<LocalDate, Map.Entry<String, Long>>> readBigQueryPackagesDownloads(CSVFormat csvFormat, List<String> names, File csvFile) {
		try {
			try(FileReader reader = new FileReader(csvFile)) {
				return csvFormat.parse(reader).stream()
						.map(this::recordToEntries)
						.collect(Collectors.collectingAndThen(
								Collectors.toList(),
								list -> this.maybeAddNewEntries(list, csvFormat, names, csvFile)
								));
			}
		} catch (IOException e) {
			throw new CannotWriteBigQueryResultsToFile("can't write table result to file " + csvFile, e);
		}
	}
	
	/**
	 * Check if the last entry of list (by localdate comparison) is less than one month old.
	 * Otherwise, perform a complementary query on Google BigQuery to get missing figures
	 * 
	 * IMPORTANT! This method has a side effect: when new entries are found, 
	 * the container file {@link #csvResultsFile} is updated with the new values 
	 * @param existingEntries
	 * @param csvFile initial CSV file
	 * @return collection containing all entries, old and news.
	 */
	private Collection<Map.Entry<LocalDate, Map.Entry<String, Long>>> maybeAddNewEntries(Collection<Map.Entry<LocalDate, Map.Entry<String, Long>>> existingEntries, CSVFormat csvFormat, List<String> names, File csvFile) {
		 Optional<LocalDate> optionalLastDate = existingEntries.stream()
				.map(Map.Entry::getKey)
				.max(Comparator.naturalOrder());
		 if(optionalLastDate.isPresent()) {
			 LocalDate lastDate = optionalLastDate.get();
			 Duration elapsed = Duration.between(lastDate.atStartOfDay(), LocalDateTime.now());
			 // A month duration is not absolute, so we emulate it :-(
			 Duration ONE_MONTH = Duration.of(31, ChronoUnit.DAYS);
			 if(elapsed.compareTo(ONE_MONTH)>0) {
				 lastDate = lastDate.plusMonths(1);
				 logger.info("Seems like BigQuery result file is too old. We need to update it");
				 // We have to add one month to the date, because the alst date available in source
				 // file has already been fetched
				 // Time to add new entries
				 String name = csvFile.getName();
				 name = name.substring(0, name.lastIndexOf('.'));
				 File update = new File(csvFile.getParentFile(), 
						 String.format("%s.update-of-%s.csv", name, DATE_FORMATTER.format(lastDate)));
				 if(update.exists()) {
					 throw new RuntimeException("update file already exists. You should merge it");
				 }
				 existingEntries.addAll(definitelyAddNewEntries(lastDate, csvFormat, names, update));
			 }
		 }
		 return existingEntries;
	}


	private Collection<? extends Entry<LocalDate, Entry<String, Long>>> definitelyAddNewEntries(LocalDate lastDate,
			CSVFormat csvFormat, List<String> names, File updated) {
		writeTableResultToCSV(csvFormat, names, lastDate, updated);
		return readBigQueryPackagesDownloads(csvFormat, names, updated);
		
	}
	/**
	 * Write the given download counts for the given project lists.
	 * @param csvFormat format used to write the file
	 * @param names names of Python packages to search
	 * @param startDate date after which we want the python packages list
	 * @param csvResultsFile File to write CSV into
	 */
	private void writeTableResultToCSV(CSVFormat csvFormat, List<String> names, LocalDate startDate, File csvResultsFile) {
		logger.info("Searching for download count for "+names.size()+" packages after "+DATE_FORMATTER.format(startDate));
		TableResult result = executeQuery(names, startDate, projectId, query);
		try {
			csvResultsFile.getParentFile().mkdirs();
			try(FileWriter writer = new FileWriter(csvResultsFile)) {
				try(CSVPrinter printer = new CSVPrinter(writer, csvFormat)) {
					for (FieldValueList row : result.iterateAll()) {
						// We can use the `get` method along with the column
						// name to get the corresponding row entry
						String date = row.get("month").getStringValue();
						String project = row.get("project").getStringValue();
						String numberOfDownloads = row.get("number_of_downloads").getStringValue();
						printer.printRecord(date, project, numberOfDownloads);
					}
				}
			}
		} catch(IOException e) {
			throw new CannotWriteBigQueryResultsToFile("Unable to write to csv file "+csvResultsFile, e);
		}
	}

	/**
	 * Execute the given BigQuery code on the given list of Python modules names
	 * @param names names of packages to scan
	 * @param startDate date after which we want to get the results
	 * @param projectId used GCP Project id
	 * @param query used query String
	 * @return a table of results (easily writable to CSV)
	 */
	private TableResult executeQuery(List<String> names,LocalDate startDate, String projectId, String query) {
		logger.info("Initializing BigQuery for "+projectId);
		BigQuery bigquery = BigQueryOptions.newBuilder().setProjectId(projectId).build().getService();

		logger.info("Configuring query\n"+query);
		final String QUERY = query;
		QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(QUERY)
				.addNamedParameter("startDate", QueryParameterValue.date(DATE_FORMATTER.format(startDate)))
				.addNamedParameter("packagesList", 
					QueryParameterValue.array(names.toArray(String[]::new), String.class))
				.build();

		logger.info("Running query");
		Job queryJob = bigquery.create(JobInfo.newBuilder(queryConfig).build());
		try {
			queryJob = queryJob.waitFor();
			// the waitFor method blocks until the job completes
			// and returns `null` if the job doesn't exist anymore
			if (queryJob == null) {
				throw new CannotExecuteBigQuery("job no longer exists");
			}
			// once the job is done, check if any error occured
			if (queryJob.getStatus().getError() != null) {
				throw new CannotExecuteBigQuery(queryJob.getStatus().getError().toString());
			}

			logger.info("Fetching results");
			TableResult result = queryJob.getQueryResults();
			return result;
		} catch (InterruptedException e) {
			throw new CannotExecuteBigQuery("Something went weirdly wrong during BigQuery search", e);
		}
	}
}
