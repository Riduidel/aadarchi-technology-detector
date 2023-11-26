///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS info.picocli:picocli:4.6.3


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import com.github.fge.lambdas.Throwing;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Response;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

//DEPS com.microsoft.playwright:playwright:1.39.0
//DEPS com.google.code.gson:gson:2.10.1
//DEPS commons-io:commons-io:2.15.0
//DEPS com.github.fge:throwing-lambdas:0.5.0
//DEPS org.apache.commons:commons-lang3:3.13.0
//DEPS org.apache.maven:maven-project:2.2.1
@Command(name = "ExtractPuploarMvnRepositoryArtifacts", mixinStandardHelpOptions = true, version = "ExtractPuploarMvnRepositoryArtifacts 0.1",
        description = "ExtractPuploarMvnRepositoryArtifacts made with jbang")
class ExtractPopularMvnRepositoryArtifacts implements Callable<Integer> {
	public static final Logger logger = Logger.getLogger(ExtractPopularMvnRepositoryArtifacts.class.getName());

	private static boolean RUNNING_ON_WINDOWS;

	private static String artifactsDetailsExtractor;
	
	static {
		RUNNING_ON_WINDOWS = System.getProperty("os.name").toLowerCase().contains("windows");
		var file = new File("artifact_details_extractor.js");
		try {
			artifactsDetailsExtractor = FileUtils.readFileToString(file, "UTF-8");
		} catch (IOException e) {
			throw new UnsupportedOperationException(String.format("Unable to load script from %s",  file.getAbsolutePath()), e);
		}
	}
	
	public static class ArtifactInformations implements Comparable<ArtifactInformations> {

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
	}
	
	public abstract class ArtifactLoader {
		private static String artifactsUrlExtractor;
		
		static {
			var file = new File("artifacts_urls_extractor.js");
			try {
				artifactsUrlExtractor = FileUtils.readFileToString(file, "UTF-8");
			} catch (IOException e) {
				throw new UnsupportedOperationException(String.format("Unable to load script from %s",  file.getAbsolutePath()), e);
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
			Gson gson = new Gson();
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

    @Option(names = {"--interesting-artifacts"}, description = "The local file containing interesting artifacts infos", defaultValue = "interesting_artifacts.json")
    private Path localInterestingArtifacts;

    @Option(names = {"--techempower-frameworks-local-clone"}, description = "The techempower frameworks local clone", defaultValue = "../../FrameworkBenchmarks/frameworks")
    private Path techEmpowerFrameworks;
    @Option(names = {"--searched-artifacts-cache"}, description = "List of already searched artifacts", defaultValue = "cached_artifacts.json")
    private Path cachedArtifacts;
    @Option(names = {"-u", "--server-url"}, description = "The used mvnrepository server", defaultValue = "https://mvnrepository.com")
    private String mvnRepositoryServer;

	private Gson gson = new GsonBuilder().setPrettyPrinting().create();

	/**
	 * Contains pat of this very script
	 */
	private File currentFile;
    public static void main(String... args) {
        int exitCode = new CommandLine(new ExtractPopularMvnRepositoryArtifacts()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
    	this.currentFile = new File(getClass().getProtectionDomain().getCodeSource().getLocation()
    		    .toURI());
        try (Playwright playwright = Playwright.create()) {
        	BrowserContext context = createPlaywrightContext(playwright);
        	var detailsPage = newPage(context);
        	logger.info("Reading popular artifacts from multiple sources");
        	List<ArtifactInformations> allArtifactInformations = null;
        	if(cachedArtifacts.toFile().exists()) {
        		var json = FileUtils.readFileToString(cachedArtifacts.toFile(), "UTF-8");
        		allArtifactInformations = gson.fromJson(artifactsDetailsExtractor, new TypeToken<List<ArtifactInformations>>() {});
        	} else {
	        	// Get the loaders
	        	allArtifactInformations = fetchArtifactInformations(context, 
	        			Arrays.asList(
	        					new LocalFileArtifactLoader(localInterestingArtifacts),
	        					new PopularArtifactLoader(),
	        					new TechEmpowerArtifactLoader(techEmpowerFrameworks)
										));
	        	var json = gson.toJson(allArtifactInformations);
	        	FileUtils.write(cachedArtifacts.toFile(), json, "UTF-8");
        	}
        	Collection allDetails = obtainAllDetails(context, allArtifactInformations);
	        logger.info("Exporting artifacts to " + output.toAbsolutePath().toFile().getAbsolutePath());
	        FileUtils.write(output.toFile(), gson.toJson(allDetails), "UTF-8");
	        logger.info(String.format("Exported %d artifacts to %s", allDetails.size(), output));
        }
        return 0;
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
						Optional<Map> details = addDetails(page, artifactInformations);
						details.stream()
							.forEach(d -> resolvedArtifacts.put(artifactInformations, d));
						return details.stream();
					} finally {
						page.close();
					}
				})
				// Now we're extracted details, let's do some complementary filtering
				.filter(artifactInformations -> ((List) artifactInformations.get("repositories")).contains("Central"))
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

	private Optional<Map> addDetails(Page page,
			ArtifactInformations artifactInformations) {
		var url = String.format("%s/artifact/%s/%s", mvnRepositoryServer, 
				artifactInformations.groupId,
				artifactInformations.artifactId);
		Response response = page.navigate(url);
		Map pageInfos = null;
		if(response.status()<300) {
			logger.info(String.format("Reading details of %s", url));
			pageInfos = ((Map) page.evaluate(artifactsDetailsExtractor));
		} else {
			logger.warning(String.format("Failed to load %s, status is %s", url, response.status()));
		}
		return Optional.ofNullable(pageInfos);
	}

	private BrowserContext createPlaywrightContext(Playwright playwright) {
    	Browser chromium = playwright.chromium().launch(
    			new BrowserType.LaunchOptions()
    				.setHeadless(false)
    			);
    	BrowserContext context = chromium.newContext();
    	context.addInitScript("Object.defineProperty(navigator, 'webdriver', {get: () => undefined})");
    	return context;
	}
}
