package org.ndx.aadarchi.technology.detector.mvnrepository;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
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
import org.ndx.aadarchi.technology.detector.model.VersionDetails;

import com.github.fge.lambdas.Throwing;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Browser.NewContextOptions;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Page.NavigateOptions;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.WaitUntilState;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
@Command(name = "ExtractPuploarMvnRepositoryArtifacts", mixinStandardHelpOptions = true, version = "ExtractPuploarMvnRepositoryArtifacts 0.1",
        description = "ExtractPuploarMvnRepositoryArtifacts made with jbang")
class ExtractPopularMvnRepositoryArtifacts implements Callable<Integer> {
	public static final Logger logger = Logger.getLogger(ExtractPopularMvnRepositoryArtifacts.class.getName());
	record ArchivePoint(String urlkey, 
    		LocalDateTime timestamp, 
    		String original, 
    		String digest,
    		String length) {
	}

	public class ArtifactInformations implements Comparable<ArtifactInformations> {

		public final String groupId;
		public final String artifactId;

		public ArtifactInformations(String groupId, String artifactId) {
			this.groupId = groupId;
			this.artifactId = artifactId;
		}
		
		@Override
		public String toString() {
			return "ArtifactInformations [groupId=" + groupId + ", artifactId=" + artifactId + "]";
		}

		@Override
		public int hashCode() {
			return Objects.hash(artifactId, groupId);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ArtifactInformations other = (ArtifactInformations) obj;
			return Objects.equals(artifactId, other.artifactId) && Objects.equals(groupId, other.groupId);
		}

		@Override
		public int compareTo(ExtractPopularMvnRepositoryArtifacts.ArtifactInformations o) {
			return new CompareToBuilder()
					.append(groupId, o.groupId)
					.append(artifactId, o.artifactId)
					.toComparison();
		}
		String toCoordinates() {
			return groupId+"."+artifactId;
		}
		String getArtifactUrl(String mvnRepositoryServer) {
			return String.format("%s/artifact/%s/%s", mvnRepositoryServer, 
					groupId,
					artifactId);
		}
	}
	
	public abstract class ArtifactLoader {
		private static String artifactsUrlExtractor;
		
		static {
			try {
				artifactsUrlExtractor = IOUtils.toString(ArtifactLoader.class.getClassLoader().getResourceAsStream("artifacts_urls_extractor.js"), "UTF-8");
			} catch (IOException e) {
				throw new UnsupportedOperationException(String.format("Unable to load script from %s", 
						ArtifactLoader.class.getClassLoader().getResource("artifacts_urls_extractor.js")), e);
			}
		}

		public abstract Set<ArtifactInformations> loadArtifacts(Page page) throws IOException;

		protected Set<ArtifactInformations> loadPageList(Page page, String url) {
			Set<ArtifactInformations> returned = new TreeSet<ArtifactInformations>();
			while(url!=null) {
				logger.info(String.format("Loading page %s", url));
				page.navigate(url);
				Map pageInfos = ((Map) page.evaluate(artifactsUrlExtractor));

	            if (!pageInfos.containsKey("data")) {
	                logger.warning(String.format("Page %s has empty data %s", url, pageInfos));
	            } else {
	            	List<Map<String, String>> data = (List) pageInfos.get("data");
	            	for (Map<String, String> object : data) {
						returned.add(new ArtifactInformations(object.get("groupId"), object.get("artifactId")));
					}
	            }
	            url = null;
	            if(pageInfos.containsKey("page")) {
	            	Map<String, String> paging = (Map<String, String>) pageInfos.get("page");
	            	if(paging.containsKey("next")) {
	            		var next  = paging.get("next");
	            		if(next!=null && !next.isBlank()) {
	            			url = paging.get("next").toString();
	            		}
	            	}
	            }
			}
			return returned;
		}
	}
	
	public class TechEmpowerArtifactLoader extends ArtifactLoader {

		private Path techEmpowerFrameworks;
		MavenXpp3Reader reader = new MavenXpp3Reader();
		
		public TechEmpowerArtifactLoader(Path techEmpowerFrameworks) {
			this.techEmpowerFrameworks = techEmpowerFrameworks;
		}

		@Override
		public Set<ArtifactInformations> loadArtifacts(Page page)
				throws IOException {
			// TODO Auto-generated method stub
			File[] children = techEmpowerFrameworks.toFile().listFiles();
			if(children.length==0) {
				throw new UnsupportedOperationException(
						String.format("There are no children in %s", techEmpowerFrameworks.toString()));
			} else {
				return Stream.of(children)
					.filter(folder -> folder.getName().equals("Java"))
					.flatMap(javaFolder -> Stream.of(javaFolder.listFiles()))
					.filter(File::isDirectory)
					.flatMap(framework -> Arrays.asList(
							new File(framework, "pom.xml"),
							new File(framework, "build.gradle")
							).stream())
					.filter(File::exists)
					.flatMap(file -> 
						file.getName().equals("pom.xml") ? 
								identifyInterestingDependenciesInMaven(file).stream() :
									identifyInterestingDependenciesInGradle(file).stream())
					.collect(Collectors.toCollection(() -> new TreeSet<ArtifactInformations>()));
			}
		}
		
		Set<ArtifactInformations> identifyInterestingDependenciesInMaven(File pomFile) {
			Set<ArtifactInformations> returned = new TreeSet<ArtifactInformations>();
			try(InputStream input = new FileInputStream(pomFile)) {
				MavenProject mavenProject = new MavenProject(reader.read(input));
				mavenProject.getModel().getDependencies().stream()
					.filter(d -> !d.getGroupId().contains("${") && !d.getArtifactId().contains("${"))
					.map(d -> new ArtifactInformations(d.getGroupId(), d.getArtifactId()))
					.peek(a -> logger.info("read artifact "+a))
					.forEach(a -> returned.add(a));
				// If pom has submodules, also explore them
				// (that could get us rid of the interesting_artifacts thingie
				mavenProject.getModel().getModules().stream()
					.forEach(module -> {
						var modulePom = new File(new File(pomFile.getParentFile(), module), "pom.xml");
						if(modulePom.exists()) {
							returned.addAll(identifyInterestingDependenciesInMaven(modulePom));
						}
					});
			} catch (IOException | XmlPullParserException e) {
				logger.log(Level.SEVERE, e, () -> String.format("unable to get informations from pom %s", pomFile));
			}
			return returned;
		}
		
		Set<ArtifactInformations> identifyInterestingDependenciesInGradle(File folder) {
			Set<ArtifactInformations> returned = new TreeSet<>();
			logger.severe("TODO implement handling of Gradle projects");
			return returned;
		}
	}
	
	public class PopularArtifactLoader extends ArtifactLoader {

		@Override
		public Set<ExtractPopularMvnRepositoryArtifacts.ArtifactInformations> loadArtifacts(Page page)
				throws IOException {
			return loadPageList(page, String.format("%s/popular", mvnRepositoryServer));
		}
		
	}
	
	/**
	 * Load interesting artifacts from a local file
	 */
	public class LocalFileArtifactLoader extends ArtifactLoader {
		private Path referenceFile;

		public LocalFileArtifactLoader(Path file) {
			this.referenceFile = file;
		}

		@Override
		public Set<ArtifactInformations> loadArtifacts(Page page) throws IOException {
			// Read the reference file
			var text = FileUtils.readFileToString(referenceFile.toFile(), "UTF-8");
			List<Map<String, String>> entries = gson.fromJson(text, List.class);
			Set<ArtifactInformations> returned = new HashSet<ArtifactInformations>();
			entries.forEach(artifact -> returned.addAll(getArtifactInformations(page, artifact)));
			return returned;
		}

		private Collection<ArtifactInformations> getArtifactInformations(Page page, Map<String, String> artifact) {
			Set<ArtifactInformations> returned = new HashSet<ArtifactInformations>();
			var groupId = artifact.get("groupId");
			if(artifact.containsKey("artifactId")) {
				returned.add(new ArtifactInformations(groupId, artifact.get("artifactId")));
			} else {
				returned.addAll(loadAllArtifactsOfGroup(page, groupId));
			}
			return returned;
		}

		private Collection<? extends ExtractPopularMvnRepositoryArtifacts.ArtifactInformations> loadAllArtifactsOfGroup(Page page, String groupId) {
			return loadPageList(page, mvnRepositoryServer+"/artifact/"+groupId);
		}
	}

    @Option(names= {"-o", "--output"}, description = "The output file for generated artifacts.json file", defaultValue = "artifacts.json")
    private Path output;

//    @Option(names = {"--interesting-artifacts"}, description = "The local file containing interesting artifacts infos", defaultValue = "interesting_artifacts.json")
//    private Path localInterestingArtifacts;

    @Option(names = {"--techempower-frameworks-local-clone"}, description = "The techempower frameworks local clone", 
    		defaultValue = "../../FrameworkBenchmarks/frameworks")
    private Path techEmpowerFrameworks;
    @Option(names = {"--searched-artifacts-cache"}, description = "List of already searched artifacts", 
    		defaultValue = "cached_artifacts.json")
    private Path cachedArtifacts;
    @Option(names = {"-u", "--server-url"}, description = "The used mvnrepository server", 
    		defaultValue = "https://mvnrepository.com")
    private String mvnRepositoryServer;
    @Option(names = {"--generate-history"}, description ="Generate an history branch with commits for each month")
    boolean generateHistory;
    @Option(names = {"--git-folder"}, description = "The output folder where data will be written", 
    		defaultValue = "../history")
    private Path gitHistory;
    @Option(names = {"--cache-folder"}, description = "Since fetching all artifacts could be very long, I prefer to manage a ocal cache, preventing the need for re-downloading everything", 
    		defaultValue = "../.cache")
    private Path cache;
    private File captures;
    private File statuses;
    private File indices;
    private File artifactsLists;

	private Gson gson = new GsonBuilder()
			.setPrettyPrinting()
			.create();

	private String artifactDetailsExtractor;


    public static void main(String... args) {
        int exitCode = new CommandLine(new ExtractPopularMvnRepositoryArtifacts()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
    	captures = new File(cache.toFile(), "captures");
    	statuses = new File(cache.toFile(), "statuses");
    	indices = new File(cache.toFile(), "indices");
    	artifactsLists = new File(cache.toFile(), "artifacts");
        try (Playwright playwright = Playwright.create()) {
        	BrowserContext context = createPlaywrightContext(playwright);
        	// An empty browser page used to make sure the browser window doesn't move over time
//        	var emptyPage = newPage(context);
        	logger.info("Reading popular artifacts from multiple sources");
        	List<ArtifactInformations> allArtifactInformations = null;
        	if(cachedArtifacts.toFile().exists()) {
        		var json = FileUtils.readFileToString(cachedArtifacts.toFile(), "UTF-8");
        		allArtifactInformations = gson.fromJson(json, new TypeToken<List<ArtifactInformations>>() {});
        	} else {
	        	// Get the loaders
	        	allArtifactInformations = fetchArtifactInformations(context, 
	        			Arrays.asList(
//	        					new LocalFileArtifactLoader(localInterestingArtifacts),
	        					new PopularArtifactLoader(),
	        					new TechEmpowerArtifactLoader(techEmpowerFrameworks)
										));
	        	var json = gson.toJson(allArtifactInformations);
	        	FileUtils.write(cachedArtifacts.toFile(), json, "UTF-8");
        	}
        	if(generateHistory) {
        		new HistoryBuilder().generateHistoryFor(context, allArtifactInformations);
        	} else {
	        	Collection allDetails = obtainAllDetails(context, allArtifactInformations);
		        logger.info("Exporting artifacts to " + output.toAbsolutePath().toFile().getAbsolutePath());
		        FileUtils.write(output.toFile(), gson.toJson(allDetails), "UTF-8");
		        logger.info(String.format("Exported %d artifacts to %s", allDetails.size(), output));
        	}
        }
        return 0;
    }
    
    private class HistoryBuilder {
		HttpClient client = HttpClient.newHttpClient();
	    /**
	     * Subverts all this class mechanism to generate a complete git history for all
	     * monthly statistics of all the given artifacts.
	     * @param context
	     * @param allArtifactInformations
	     * @throws IOException 
	     */
	    private void generateHistoryFor(BrowserContext context,
				List<ExtractPopularMvnRepositoryArtifacts.ArtifactInformations> allArtifactInformations) throws IOException {
	    	Git git = Git.open(gitHistory.toFile());
	    	// First thing first, fill our cache with all remote infos we can have
	    	Map<ArtifactInformations, NavigableMap<LocalDate, File>> artifactsCaptures = allArtifactInformations.stream()
	    		.collect(Collectors.toMap(
	    				Function.identity(), 
	    				Throwing.function(artifact -> cacheHistoryOf(context, artifact)),
	    				(firstMap, secondMap) -> firstMap
	    				));
	    	// Print some statistics
	    	logger.info("Processed artifacts\n" +
	    			artifactsCaptures.entrySet().stream()
	    				.map(e -> String.format("%s -> %d captures", e.getKey(), e.getValue().size()))
	    				.collect(Collectors.joining("\n"))
	    				);
	    	List<ArtifactDetails> allArtifactsDetails = (List<ArtifactDetails>) gson.fromJson(
	    			FileUtils.readFileToString(output.toFile(), "UTF-8"),
	    			TypeToken.getParameterized(List.class, ArtifactDetails.class));
	    	// Move history details from the future to the past for the whole timeline
	    	artifactsCaptures.entrySet().forEach(entry -> copyHistoryFromLastToFirst(entry.getKey(),
	    			allArtifactsDetails,
	    			entry.getValue()));
	    	Collection<ArtifactDetails> currentArtifacts = gson.fromJson(FileUtils.readFileToString(output.toFile(), "UTF-8"),
	    			new TypeToken<Collection<ArtifactDetails>>() {});
	    	// Generate complete artifact informations through inference
	    	Map<ArtifactInformations, NavigableMap<LocalDate, File>> artifactsStatuses = artifactsCaptures.entrySet().stream()
	    			.collect(Collectors.toMap(
	    					entry -> entry.getKey(),
	    					Throwing.function(entry -> generateAllStatusesFrom(entry.getKey(), entry.getValue(), currentArtifacts))));
	    	// Create one artifacts.json per month
	    	var aggregatedStatuses = aggregateStatusesPerDate(artifactsStatuses);
	    	// Write them into git history
	    	aggregatedStatuses.entrySet().stream()
	    		.forEach(Throwing.consumer(entry1 -> writeArtifactListAtDate(git, entry1.getKey(), entry1.getValue())));
		}
	    private void writeArtifactListAtDate(Git git, LocalDate date,
				SortedSet<ArtifactDetails> value) throws IOException, AbortedByHookException, ConcurrentRefUpdateException, NoHeadException, NoMessageException, ServiceUnavailableException, UnmergedPathsException, WrongRepositoryStateException, GitAPIException {
	    	// First, write the artifact in its own folder
	    	File datedFilePath = getDatedFilePath(artifactsLists, date, "artifacts");
	    	logger.info(String.format("Writing %d artifacts of %s into %s", value.size(), Formats.MVN_DATE_FORMAT_WITH_DAY.format(date), datedFilePath));
			FileUtils.write(datedFilePath, 
	    			gson.toJson(value), "UTF-8");
	    	File artifacts = new File(new File(gitHistory.toFile(), "mvnrepository"), "artifacts.json");
	    	FileUtils.copyFile(datedFilePath, artifacts);
	    	// Then create a commit in the history repository
	    	ZoneId systemZoneId = ZoneId.systemDefault();
			Instant commitInstant = date.atStartOfDay(systemZoneId).toInstant();
			PersonIdent commiter = new PersonIdent("🤖 MvnRepository History Builder", 
					"get_mvnrepository_infos.yaml@history",
	    			commitInstant, systemZoneId);
			git.add()
				.addFilepattern("resources/mvnrepository/artifacts.json")
				.call();
			git.commit()
	    		.setAuthor(commiter)
	    		.setCommitter(commiter)
	    		.setAll(true)
//	    		.setOnly("mvnrepository/artifacts.json")
//	    		.setAllowEmpty(false)
	    		.setMessage(String.format("Historical artifacts of %s, %d artifacts known at this date", 
	    				Formats.MVN_DATE_FORMAT_WITH_DAY.format(date), value.size()))
	    		.call()
	    		;
		}
		/**
	     * Aggregate statuses per month, write the artifacts.json files and put them into a per-date map (for creating the git commits)
	     * @param artifactsStatuses
	     * @return
	     * @throws IOException 
	     * @throws JsonSyntaxException 
	     */
	    private Map<LocalDate, SortedSet<ArtifactDetails>> aggregateStatusesPerDate(
				Map<ArtifactInformations, NavigableMap<LocalDate, File>> artifactsStatuses) throws JsonSyntaxException, IOException {
	    	// We directly take values, because they should all already be first of month ones
	    	Map<LocalDate, SortedSet<ArtifactDetails>> artifactLists = new TreeMap<>();
	    	for(Map<LocalDate, File> map : artifactsStatuses.values()) {
	    		for (Entry<LocalDate, File> entry: map.entrySet()) {
					LocalDate key = entry.getKey();
					if(!artifactLists.containsKey(key)) {
						artifactLists.put(key, new TreeSet<>());
					}
					artifactLists.get(key).add(gson.fromJson(FileUtils.readFileToString(entry.getValue(), "UTF-8"), ArtifactDetails.class));
				}
	    	}
	    	return artifactLists;
		}
		private void copyHistoryFromLastToFirst(ArtifactInformations key,
				List<ArtifactDetails> allArtifactInformations,
				NavigableMap<LocalDate, File> value) {
	    	Optional<ArtifactDetails> associatedArtifactDetail = allArtifactInformations.stream()
	    			.filter(artifact -> artifact.coordinates.equals(key.toCoordinates()))
	    			.findFirst()
	    			;
	    	if(associatedArtifactDetail.isPresent()) {
	    		copyHistoryFromLastToFirst(key, associatedArtifactDetail.get(), value);
	    	} else {
	    		logger.warning(String.format("Unable to copy history from missing last artifact %s", key));
	    	}
		}
		/**
	     * Copy fine release date from the last element to the first in the whole artifact history.
	     * Because MvnRepository contains release dates only for recent versions
	     * @param artifact
	     * @param capturesHistory
	     */
	    private void copyHistoryFromLastToFirst(ArtifactInformations artifact,
	    		ArtifactDetails details,
				NavigableMap<LocalDate, File> capturesHistory) {
	    	ArtifactDetails newDetails = null;
	    	capturesHistory.forEach((date, file) -> copyDatesInto(details, date, file));
		}
	    /**
	     * Copy the versions date from the details object into the oldDetails object (which will obviously be changed)
	     */
	    private void copyDatesInto(ArtifactDetails details, LocalDate date, File file) {
    		try {
	    		ArtifactDetails oldDetails = gson.fromJson(FileUtils.readFileToString(file, "UTF-8"), ArtifactDetails.class);
    			Optional<ArtifactDetails> changedDetailsOptiona = oldDetails.copyDatesFrom(details);
    			if(changedDetailsOptiona.isPresent()) {
    				ArtifactDetails changedDetails = changedDetailsOptiona.get();
    				FileUtils.write(file, gson.toJson(changedDetails), "UTF-8");
    				oldDetails = changedDetails;
    			}
    		} catch(IOException e) {
    			throw new UnsupportedOperationException("Unable to process some file", e);
    		}
	    }
		/** Generate a list of status for each month (since first known version) for the given artifact.
	     * For that, usage will be infered through linear regression between the various most recent points. 
	     * When no usage can be found, well, we imagine something
	     * @param artifact
	     * @param knownHistory
	     * @return
	     * @throws IOException 
	     * @throws JsonSyntaxException 
	     */
	    public NavigableMap<LocalDate, File> generateAllStatusesFrom(ArtifactInformations artifact, 
	    		NavigableMap<LocalDate, File> knownHistory,
	    		Collection<ArtifactDetails> currentArtifacts) throws JsonSyntaxException, IOException {
	    	logger.info(String.format("generating all statuses for %s", artifact.toCoordinates()));
	    	NavigableMap<LocalDate, File> returned = new TreeMap<LocalDate, File>();
	    	LocalDate now = LocalDate.now();
	    	LocalDate currentMonth = LocalDate.of(now.getYear(), now.getMonthValue(), 1);
	    	LocalDate startMonth = LocalDate.of(2006, 1, 1);
	    	while(currentMonth.compareTo(startMonth)>0) {
	    		Optional<File> capture = generateStatusFor(currentMonth, artifact, knownHistory, currentArtifacts);
	    		if(capture.isPresent()) {
	    			returned.put(currentMonth, capture.get());
	    		}
	    		currentMonth = currentMonth.minusMonths(1);
	    	}
	    	return returned;
	    }
		private Optional<File> generateStatusFor(LocalDate currentMonth, ArtifactInformations artifact,
				NavigableMap<LocalDate, File> knownHistory, Collection<ArtifactDetails> currentArtifacts) throws JsonSyntaxException, IOException {
    		File output = getDatedFilePath(statuses, currentMonth, artifact.toCoordinates());
    		if(output.exists())
    			return Optional.of(output);
    		Entry<LocalDate, File> fileSnapshotBefore = knownHistory.floorEntry(currentMonth);
    		Entry<LocalDate, File> fileSnapshotAfter = knownHistory.ceilingEntry(currentMonth);
    		ArtifactDetails dataBefore = null;
    		ArtifactDetails dataAfter = null;
    		Optional<ArtifactDetails> currentData = currentArtifacts.stream()
    				.filter(a -> a.coordinates.equals(artifact.toCoordinates()))
    				.findFirst();
    		if(currentData.isEmpty())
    			return Optional.empty();
    		if(fileSnapshotBefore!=null) {
    			if(fileSnapshotBefore.getKey().compareTo(currentMonth)<0) {
    				dataBefore = gson.fromJson(FileUtils.readFileToString(fileSnapshotBefore.getValue(), "UTF-8"), ArtifactDetails.class);
    			} else {
    				fileSnapshotBefore = null;
    			}
    		}
    		LocalDate dateAfter = null;
    		if(fileSnapshotAfter==null) {
    			dateAfter = currentMonth;
   				dataAfter = currentData.get();
    		} else {
    			dataAfter = gson.fromJson(FileUtils.readFileToString(fileSnapshotAfter.getValue(), "UTF-8"), ArtifactDetails.class);
    			dateAfter = fileSnapshotAfter.getKey();
    		}
    		if(fileSnapshotBefore==null)
    			return Optional.empty();
    		// First step is simple: fill missing data
    		if(dataAfter!=null) {
	    		ArtifactDetails currentMonthData =
	    				fileSnapshotBefore==null ?
		    				dataAfter.inferDataPoint(currentData.get(), dateAfter, currentMonth, null, null)
		    				: dataAfter.inferDataPoint(currentData.get(), dateAfter, currentMonth, fileSnapshotBefore.getKey(), dataBefore);
	    		// Now we have an artifact, write it to disk
	    		FileUtils.write(output, gson.toJson(currentMonthData), "UTF-8");
	    		return Optional.of(output);
    		}
			return Optional.empty();
		}
		/**
	     * Obtain the artifact history and cache it locally
	     * @param context
	     * @param artifact
	     * @return true if all entries have been processed successfully (files are present on machine for each entry)
	     * @throws URISyntaxException 
	     * @throws InterruptedException 
	     * @throws IOException 
	     */
		private NavigableMap<LocalDate, File> cacheHistoryOf(BrowserContext context,
				ExtractPopularMvnRepositoryArtifacts.ArtifactInformations artifact) throws IOException, InterruptedException, URISyntaxException {
			var history = getArtifactHistoryOnCDX(artifact);
			return history.stream()
				.map(archivePoint -> 
					obtainArchivePointFor(artifact, archivePoint, context))
				.filter(entry -> entry!=null)
				.filter(entry -> entry.getValue()!=null)
				.collect(Collectors.toMap(entry -> entry.getKey().toLocalDate(), 
						entry -> entry.getValue(),
						(a, b) -> a,
						() -> new TreeMap<>()));
		}
		
		
	
		private Entry<LocalDateTime, File> obtainArchivePointFor(
				ArtifactInformations artifact,
				ExtractPopularMvnRepositoryArtifacts.ArchivePoint archivePoint,
				BrowserContext context) {
				File destination = getDatedFilePath(captures, archivePoint.timestamp.toLocalDate(), artifact.toCoordinates());
				if(destination.exists()) {
					if(destination.length()<200) {
						logger.warning(String.format("Seems like artifact %s has no content. Removing it.", archivePoint));
						destination.delete();
					} else {
						return Map.entry(archivePoint.timestamp, destination);
					}
				}
				var url = String.format("https://web.archive.org/web/%s/%s", 
						Formats.INTERNET_ARCHIVE_DATE_FORMAT.format(archivePoint.timestamp),
						artifact.getArtifactUrl(mvnRepositoryServer));
				try (Page page = newPage(context)){
					
					var details = addDetails(page, url);
					details.ifPresent(Throwing.consumer(
							map -> {
						var json = gson.toJson(map);
						FileUtils.write(destination, json, "UTF-8");
					}));
					return Map.entry(archivePoint.timestamp, destination);
				} catch(PlaywrightException e) {
					logger.log(Level.WARNING, 
							String.format("Unable to add cache entry %s due to %s", archivePoint, e.getMessage()));
				}
				return null;
		}
		private File getDatedFilePath(File containerDir,
				LocalDate timestamp, String name) {
			return FileUtils.getFile(containerDir, 
					timestamp.getYear()+"", 
					timestamp.getMonthValue()+"",
					timestamp.getDayOfMonth()+"",
					name+".json");
		}
	
		private List<ArchivePoint> getArtifactHistoryOnCDX(ExtractPopularMvnRepositoryArtifacts.ArtifactInformations artifact) throws IOException, InterruptedException, URISyntaxException {
			File cache = new File(indices, artifact.toCoordinates()+".json");
			String url = String.format("http://web.archive.org/cdx/search/cdx"
					+ "?filter=statuscode:200" // We take only valid archive.org captures
					+ "&output=json"
					+ "&collapse=timestamp:6" // We look at the month scale
					+ "&url=%s", artifact.getArtifactUrl(mvnRepositoryServer));
			if(!cache.exists()) {
				// Run request
				try {
					HttpResponse<String> response = client.send(HttpRequest.newBuilder(new URI(url))
							.build(), BodyHandlers.ofString());
					if(response.statusCode()<300) {
						String text = response.body();
						FileUtils.write(cache, text, "UTF-8");
					} else {
						throw new UnsupportedOperationException(String.format("Can't get content from webarchive with status code %d", response.statusCode()));
					}
				} catch(IOException e) {
					logger.log(Level.WARNING,
							String.format("Couldn't get history for %s due to %s", artifact, e.getMessage()));
					return Arrays.asList();
				}
			}
			String text = FileUtils.readFileToString(cache, "UTF-8");
			return gson.fromJson(text, new TypeToken<List<List<String>>>() {}).stream()
					.filter(row -> !row.get(0).equals("urlkey"))
					.map(Throwing.function(
							row -> new ArchivePoint(row.get(0), 
							LocalDateTime.parse(row.get(1), Formats.INTERNET_ARCHIVE_DATE_FORMAT),
							row.get(2),
							row.get(5),
							row.get(6))))
					.sorted(Comparator.comparing(ArchivePoint::timestamp))
					.collect(Collectors.toList());
		}
    }

	private List<ArtifactInformations> findRelocated(Map artifactInformations) {
    	if(artifactInformations.containsKey("relocation")) {
    		Map relocation = (Map) artifactInformations.get("relocation");
    		Map<String, String> page = (Map<String, String>) relocation.get("page");
    		var groupId = page.get("groupId");
    		var artifactId = page.get("artifactId");
    		var relocated = new ArtifactInformations(groupId, artifactId);
    		return List.of(relocated);
    	} else {
    		return List.of();
    	}
    }

	private Collection<Map> obtainAllDetails(BrowserContext context,
			List<ArtifactInformations> allArtifactInformations) {
		Map<ArtifactInformations, Map> resolvedArtifacts = new TreeMap<ArtifactInformations, Map>();
		while(!allArtifactInformations.isEmpty()) {
			allArtifactInformations = allArtifactInformations.stream()
				.flatMap(artifactInformations -> {
					Page page = newPage(context);
					try {
						Optional<Map> details = addDetails(page, artifactInformations.getArtifactUrl(mvnRepositoryServer));
						details.stream()
							.forEach(d -> resolvedArtifacts.put(artifactInformations, d));
						return details.stream();
					} finally {
						page.close();
					}
				})
				// Now we're extracted details, let's do some complementary filtering
				.filter(artifactInformations -> artifactInformations.containsKey("repositories") ? ((List) artifactInformations.get("repositories")).contains("Central") : false)
				.flatMap(artifactInformations -> findRelocated(artifactInformations).stream())
				.filter(artifactInformations -> !resolvedArtifacts.containsKey(artifactInformations))
				.collect(Collectors.toList());
		}
		return resolvedArtifacts.values();
	}

	private List<ArtifactInformations> fetchArtifactInformations(
			BrowserContext context, List<ArtifactLoader> sources) {
		return sources.stream()
			.map(Throwing.function(source -> this.loadArtifacts(context, source)))
			.flatMap(artifactInformationsSet -> artifactInformationsSet.stream())
			.peek(artifactInformations -> logger.info(String.format("found artifact %s", artifactInformations)))
			.sorted()
			.collect(Collectors.toList());
	}

	private Set<ArtifactInformations> loadArtifacts(BrowserContext context, ArtifactLoader source) throws IOException {
		Page page = newPage(context);
		try {
			return source.loadArtifacts(page);
		} finally {
			page.close();
		}
	}

	private Optional<Map> addDetails(Page page,
			String url) {
		Response response = page.navigate(url, new NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
		Map pageInfos = null;
		if(response!=null && response.status()<300) {
			logger.info(String.format("Reading details of %s", url));
			pageInfos = ((Map) page.evaluate(getArtifactDetailsExtractor()));
		} else {
			logger.warning(String.format("Failed to load %s, status is %s", url, response.status()));
		}
		return Optional.ofNullable(pageInfos);
	}

	private String getArtifactDetailsExtractor() {
		if(artifactDetailsExtractor==null) {
			try {
				artifactDetailsExtractor = IOUtils.toString(ArtifactLoader.class.getClassLoader().getResourceAsStream("artifact_details_extractor.js"), "UTF-8");
			} catch (IOException e) {
				throw new UnsupportedOperationException("can't read file");
			}
		}
		return artifactDetailsExtractor;
	}

	private BrowserContext createPlaywrightContext(Playwright playwright) {
    	Browser chromium = playwright.chromium().launch(
    			new BrowserType.LaunchOptions()
    				.setHeadless(false)
    			);
    	BrowserContext context = chromium.newContext(new NewContextOptions()
    			.setJavaScriptEnabled(false));
		// Disable all resources coming from any domain that is not
		// mvnrepository or wayback machine
//		context.route(url -> !(url.contains("mvnrepository.com") || url.contains("web.archive.com")), 
//				route -> route.abort());
    	context.addInitScript("Object.defineProperty(navigator, 'webdriver', {get: () => undefined})");
    	context.setDefaultTimeout(0);
    	return context;
	}


	/**
	 * Creates a correctly configured page
	 * @param context
	 * @return
	 */
	private Page newPage(BrowserContext context) {
		Page created = context.newPage();
		created.onConsoleMessage(message -> {
			Level level = switch(message.type()) {
			case "debug" -> Level.FINE;
			case "warning" -> Level.WARNING;
			case "error" -> Level.SEVERE;
			default -> Level.INFO;
			};
			logger.logp(level, "Playwright", message.location(), () -> message.text());
		});
		return created;
	}
}
