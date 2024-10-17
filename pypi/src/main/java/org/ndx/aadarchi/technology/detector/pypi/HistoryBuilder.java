package org.ndx.aadarchi.technology.detector.pypi;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Function;
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
import org.ndx.aadarchi.technology.detector.pypi.exception.PypiBigQueryExecutionException;
import org.ndx.aadarchi.technology.detector.pypi.exception.PypiCSVProcessingException;
import org.ndx.aadarchi.technology.detector.pypi.exception.PypiQueryLoadException;

public class HistoryBuilder extends BaseHistoryBuilder<PypiContext> {
	public static final Logger logger = Logger.getLogger(HistoryBuilder.class.getName());
	private final String query;
	private final String projectId;
	private File csvResultsFile;
	private boolean reduced;
	private File historyBaseFolder;

	public HistoryBuilder(Path gitHistory, Path cache, String projectId, String queryName) {
		super(cache, "🤖 Pypi History Builder", "get_pypi_infos.yaml@history", "pypi");
		this.projectId = projectId;
		reduced = queryName.contains("reduced");
		query = loadQuery(queryName);
		csvResultsFile = new File("src/main/resources/big_query_results/"+queryName+".csv");
		historyBaseFolder = new File(cache.toFile(), "history");
	}

	private String loadQuery(String queryName) {
		Properties properties = new Properties();
		try {
			try (InputStream queryStream = getClass().getClassLoader().getResourceAsStream("query.xml")) {
				properties.loadFromXML(queryStream);
				return properties.getProperty(queryName);
			}
		} catch (IOException e) {
			throw new PypiQueryLoadException("Unable to open query.xml", e);
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

	private SortedMap<LocalDate, File> runBigQuery(Collection<ArtifactDetails> allArtifactInformations) {
		List<String> names = allArtifactInformations.stream().map(ArtifactDetails::getName)
				.collect(Collectors.toList());
		return getPackagesAsCSVResult(names,
			result -> processResults(allArtifactInformations, result));
	}
	
	private SortedMap<LocalDate, File> processResults(Collection<ArtifactDetails> allArtifactInformations, Iterable<CSVRecord> result) {
		Map<LocalDate, Map<String, Long>> groupedByDate = 
				StreamSupport.stream(result.spliterator(), false)
			.map(this::recordToEntries)
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
		FileHelper.writeToFile(artifactsAtDateCollection, artifactsAtDateFile);
		return Map.entry(month, artifactsAtDateFile);
	}

	private Map.Entry<LocalDate, Map.Entry<String, Long>> recordToEntries(CSVRecord r) {
		LocalDate month = LocalDate.from(DateTimeFormatter.ISO_LOCAL_DATE.parse(r.get("month")));
		String project = r.get("project");
		long downloads = Long.parseLong(r.get("number_of_downloads"));
		return Map.entry(month, Map.entry(project, downloads));
	}

	private <Type> Type getPackagesAsCSVResult(List<String> names, Function<Iterable<CSVRecord>, Type> function) {
		CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
				.setHeader("month", "project", "number_of_downloads")
				.setSkipHeaderRecord(true)
				.build();
		if (!csvResultsFile.exists()) {
			writeTableResultToCSV(csvFormat, names);
		}
		try {
			try(FileReader reader = new FileReader(csvResultsFile)) {
				return function.apply(csvFormat.parse(reader));
			}
		} catch (IOException e) {
			throw new PypiCSVProcessingException("can't write table result to file " + csvResultsFile, e);
		}
	}

	private void writeTableResultToCSV(CSVFormat csvContainer, List<String> names) {
		TableResult result = executeQuery(names);
		try {
			csvResultsFile.getParentFile().mkdirs();
			try(FileWriter writer = new FileWriter(csvResultsFile)) {
				try(CSVPrinter printer = new CSVPrinter(writer, csvContainer)) {
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
			throw new PypiCSVProcessingException("Unable to write to csv file "+csvResultsFile, e);
		}
	}

	private TableResult executeQuery(List<String> names) {
		logger.info("Initializing BigQuery for "+projectId);
		BigQuery bigquery = BigQueryOptions.newBuilder().setProjectId(projectId).build().getService();

		logger.info("Configuring query\n"+query);
		final String QUERY = query;
		QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(QUERY)
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
				throw new PypiBigQueryExecutionException("job no longer exists");
			}
			// once the job is done, check if any error occured
			if (queryJob.getStatus().getError() != null) {
				throw new PypiBigQueryExecutionException(queryJob.getStatus().getError().toString());
			}

			logger.info("Fetching results");
			TableResult result = queryJob.getQueryResults();
			return result;
		} catch (InterruptedException e) {
			throw new PypiBigQueryExecutionException("Something went weirdly wrong during BigQuery search", e);
		}
	}
}
