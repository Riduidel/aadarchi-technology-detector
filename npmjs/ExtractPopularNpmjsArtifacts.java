///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS info.picocli:picocli:4.6.3

//DEPS com.google.code.gson:gson:2.10.1
//DEPS commons-io:commons-io:2.15.0
//DEPS com.github.fge:throwing-lambdas:0.5.0
//DEPS org.apache.commons:commons-lang3:3.13.0
//DEPS org.eclipse.jgit:org.eclipse.jgit:6.7.0.202309050840-r


import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

import com.github.fge.lambdas.Throwing;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "ExtractPopularNpmjsArtifacts", mixinStandardHelpOptions = true, version = "ExtractPopularNpmjsArtifacts 0.1",
        description = "ExtractPopularNpmjsArtifacts made with jbang")
class ExtractPopularNpmjsArtifacts implements Callable<Integer> {
	private static final Logger logger = Logger.getLogger(ExtractPopularNpmjsArtifacts.class.getName());
	/**
	 * @see https://github.com/npm/registry/blob/main/docs/download-counts.md
	 */
	private static final String DOWNLOADS_LAST_MONTH = "https://api.npmjs.org/downloads/point/%s/%s";
	HttpClient client = HttpClient.newHttpClient();
	public static Gson gson = new GsonBuilder()
			.setPrettyPrinting()
			.create();

    @Option(names= {"-o", "--output"}, description = "The output file for generated artifacts.json file", defaultValue = "artifacts.json")
    private Path output;
    @Option(names = {"--generate-history"}, description ="Generate an history branch with commits for each month")
    boolean generateHistory;

	@Option(names = {"--cache-folder"}, description = "Since fetching all artifacts could be very long, I prefer to manage a ocal cache, preventing the need for re-downloading everything", 
    		defaultValue = "../.cache/npmjs")
    private Path cache;
    @Option(names = {"--git-folder"}, description = "The output folder where data will be written", 
    		defaultValue = "../history")
    private Path gitHistory;

    public interface ArtifactLoader {

		public default List<ArtifactInformations> loadArtifacts() throws Exception {
			File cachedArtifacts = getCachedArtifactsFile();
			if(cachedArtifacts.lastModified()<System.currentTimeMillis()-(1000*60*60*24)) {
				cachedArtifacts.delete();
			}
			if(!cachedArtifacts.exists()) {
				writeArtifacts(doLoadArtifacts(), cachedArtifacts);
			}
			return readArtifacts(cachedArtifacts);
		}

		public List<ArtifactInformations> doLoadArtifacts() throws Exception;

		public File getCachedArtifactsFile();
		
	}

	public static class ArtifactInformations {
		public String name;
		public String version;
		public String description;
		public List<String> keywords = new ArrayList<String>();
		public String date;
		public Map<String, String> links = new TreeMap<String, String>();
		public long downloads;
		@Override
		public String toString() {
			return "ArtifactInformations [name=" + name + ", version=" + version + ", keywords=" + keywords + "]";
		}
		public ArtifactInformations withDownloads(
				ExtractPopularNpmjsArtifacts.DownloadCount jsonResponse) {
			ExtractPopularNpmjsArtifacts.ArtifactInformations returned = new ArtifactInformations();
			returned.name = name;
			returned.version = version;
			returned.description = description;
			returned.keywords = new ArrayList<String>(keywords);
			returned.date = date;
			returned.links = new TreeMap<String, String>(links);
			returned.downloads = jsonResponse.downloads;
			return returned;
		}
	}
	
	public class PopularNpmArtifacts implements ArtifactLoader {
		private File cachedArtifacts = new File(cache.toFile(), "popularNpmArtifacts.json");
		public static final List<String> POPULAR_ARTIFACTS_PAGES = 
				IntStream.of(0, 251, 501, 751)
				.mapToObj(index -> String.format("https://registry.npmjs.com/-/v1/search?text=not:unstable&popularity=1.0&from=%d&size=250", index))
				.collect(Collectors.toList())
				;
		
		public static class PackageResponse {
			@SerializedName("package")
			public ArtifactInformations artifact;
		}
		
		public static class NpmJsResponse {
			public List<PackageResponse> objects;
			public long total;
			public String time;
		}

		@Override
		public List<ArtifactInformations> doLoadArtifacts() throws IOException, InterruptedException {
			return POPULAR_ARTIFACTS_PAGES.stream()
				.map(Throwing.function(this::doLoadArtifactsFrom))
				.flatMap(List::stream)
				.collect(Collectors.toList());
		}
		
		List<ArtifactInformations> doLoadArtifactsFrom(String url) throws IOException, InterruptedException {
			HttpRequest request = HttpRequest.newBuilder(URI.create(url)).build();
			HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
			NpmJsResponse jsonResponse = gson.fromJson(response.body(), new TypeToken<NpmJsResponse>() {});
			List<ExtractPopularNpmjsArtifacts.ArtifactInformations> returned = jsonResponse.objects.stream()
					.map(a -> a.artifact)
					.collect(Collectors.toList());
	        return returned;

		}

		@Override
		public File getCachedArtifactsFile() {
			return cachedArtifacts;
		}
		
	}
    
	private Set<ArtifactInformations> fetchArtifactInformations(
			List<ArtifactLoader> sources) {
		return sources.stream()
			.map(Throwing.function(source -> source.loadArtifacts()))
			.flatMap(artifactInformationsSet -> artifactInformationsSet.stream())
			.peek(artifactInformations -> logger.info(String.format("found artifact %s", artifactInformations)))
			.sorted(Comparator.comparing((ArtifactInformations a) -> a.name))
			.collect(Collectors.toSet());
	}

	public static void main(String... args) {
        int exitCode = new CommandLine(new ExtractPopularNpmjsArtifacts()).execute(args);
        System.exit(exitCode);
    }
	
	class HistoryBuilder {

		public static DateTimeFormatter DATE_FORMAT_WITH_DAY =
				new DateTimeFormatterBuilder()
					.appendPattern("MMM dd, yyyy")
					.parseCaseInsensitive()
					.toFormatter(Locale.ENGLISH)
					;

		public void generateHistoryFor(Collection<ArtifactInformations> allDetails) throws IOException {
			logger.info("Opening git repository at "+gitHistory.toFile().getAbsolutePath());
	    	Git git = Git.open(gitHistory.toFile());
	    	LocalDate initial = LocalDate.of(2010, 1, 1);
	    	logger.info("Fetching all dependencies since "+initial);
	    	Map<LocalDate, File> aggregatedStatuses = initial.datesUntil(LocalDate.now(), Period.ofMonths(1))
	    		.map(Throwing.function(month -> Map.entry(month, generateHistoryAtMonth(allDetails, month))))
	    		.filter(Throwing.predicate(entry -> {
	    			Collection<ArtifactInformations> infos = readArtifacts(entry.getValue());
	    			return !infos.isEmpty();
	    		}))
//	    		.forEach(entry -> logger.info("Got downloads at "+entry.getKey()))
	    		.collect(Collectors.toMap(Entry::getKey, Entry::getValue, (a, b) -> a, () -> new LinkedHashMap<LocalDate, File>()))
	    		;
	    	logger.info("Creating commits for all dates since "+initial);
		    
	    	// Write them into git history
	    	aggregatedStatuses.entrySet().stream()
	    		.forEach(Throwing.consumer(entry1 -> writeArtifactListAtDate(git, entry1.getKey(), entry1.getValue())));

		}
	    private void writeArtifactListAtDate(Git git, LocalDate date,
				File datedFilePath) throws IOException, AbortedByHookException, ConcurrentRefUpdateException, NoHeadException, NoMessageException, ServiceUnavailableException, UnmergedPathsException, WrongRepositoryStateException, GitAPIException {
	    	logger.info("Creating commit at "+date);
			Collection<ArtifactInformations> infos = readArtifacts(datedFilePath);
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

		private File  generateHistoryAtMonth(Collection<ExtractPopularNpmjsArtifacts.ArtifactInformations> allDetails,
				LocalDate month) throws IOException {
			File destination = getDatedFilePath(new File(cache.toFile(), "captures"), month);
			if(!destination.exists()) {
				DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd");
				String monthlySearch = String.format("%s:%s",
						format.format(month),
						format.format(month.withDayOfMonth(month.getMonth().length(month.isLeapYear())))
						);
				Collection<ExtractPopularNpmjsArtifacts.ArtifactInformations> atMonth = getAllDownloadsForPeriod(allDetails, monthlySearch);
		        writeArtifacts(atMonth, destination);

			}
			return destination;
		}
		
	}

    @Override
    public Integer call() throws Exception {
    	Collection<ArtifactInformations> allDetails = fetchArtifactInformations(Arrays.asList(
    			// Way to much complicated
//    			new CodebaseShowArtifacts(),
    			new PopularNpmArtifacts()));
    	// Changing this period will allow us to build very fast a very good history
    	if(generateHistory) {
    		new HistoryBuilder().generateHistoryFor(allDetails);
    		
    	} else {
	    	String period = "last-month";
	    	allDetails = getAllDownloadsForPeriod(allDetails, period);
	        writeArtifacts(allDetails, output.toFile());
    	}
        return 0;
    }

    static List<ArtifactInformations> readArtifacts(File file) throws IOException {
    	return gson.fromJson(FileUtils.readFileToString(file, "UTF-8"),
				new TypeToken<List<ArtifactInformations>>() {});
    }
	static void writeArtifacts(Collection<ArtifactInformations> allDetails, File file) throws IOException {
		logger.info("Exporting artifacts to " + file.getAbsolutePath());
		FileUtils.write(file, gson.toJson(allDetails), "UTF-8");
		logger.info(String.format("Exported %d artifacts to %s", allDetails.size(), file));
	}

	/**
	 * Get all downloads on a given period, as defined by npmjs download api
	 * @param artifactsToQuery
	 * @param period the period of time to get downloads for
	 * @return list of artifacts having at least one download on this period
	 * @throws IOException
	 */
	private Collection<ArtifactInformations> getAllDownloadsForPeriod(Collection<ArtifactInformations> artifactsToQuery, String period) throws IOException {
		// Now get download count for last month
    	return artifactsToQuery.stream()
    		// Do not use parallel, cause the download count api is quite cautious on load and will fast put an hauld on our queries
//    		.parallel()
    		.map(Throwing.function(artifact -> countDownloadsOf(artifact, period)))
    		.filter(artifact -> artifact.downloads>0)
			.sorted(Comparator.comparing((ArtifactInformations a) -> a.name))
    		.collect(Collectors.toList());
	}
    
    private static class DownloadCount {
    	  long downloads;
    	  String start;
    	  String end;
    	  @SerializedName("package")
    	  String packageName;
    }

    /**
     * Create an artifact with download count for the given artifact
     * @param artifact
     * @return
     * @throws InterruptedException 
     * @throws IOException 
     */
	private ArtifactInformations countDownloadsOf(ExtractPopularNpmjsArtifacts.ArtifactInformations artifact, String period) throws IOException, InterruptedException {
		logger.info(String.format("Getting downloads counts of %s for period %s", 
				artifact.name, period));
		int status = 1000;
		do {
			HttpRequest request = HttpRequest.newBuilder(URI.create(
					String.format(DOWNLOADS_LAST_MONTH, period, artifact.name))).build();
			try {
				HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
				status = response.statusCode();
				DownloadCount jsonResponse = gson.fromJson(response.body(), new TypeToken<DownloadCount>() {});
				return artifact.withDownloads(jsonResponse);
			} catch(Exception e) {
				logger.log(Level.WARNING, "Seems like we were hit by an error. Let's wait some time and retry latter ...");
				synchronized(this) {
					this.wait(5*60*1000);
				}
			}
		} while(status>=400);
		return artifact;
	}
}
