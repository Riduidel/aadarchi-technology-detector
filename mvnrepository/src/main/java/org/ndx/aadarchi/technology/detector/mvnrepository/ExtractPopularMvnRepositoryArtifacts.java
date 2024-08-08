package org.ndx.aadarchi.technology.detector.mvnrepository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.ndx.aadarchi.technology.detector.model.VersionDetails;

import com.github.fge.lambdas.Throwing;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Browser.NewContextOptions;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Page.NavigateOptions;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.WaitUntilState;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
@Command(name = "ExtractPuploarMvnRepositoryArtifacts", mixinStandardHelpOptions = true, version = "ExtractPuploarMvnRepositoryArtifacts 0.1",
        description = "ExtractPuploarMvnRepositoryArtifacts made with jbang")
class ExtractPopularMvnRepositoryArtifacts implements Callable<Integer> {
	public static final Logger logger = Logger.getLogger(ExtractPopularMvnRepositoryArtifacts.class.getName());
	

	@Option(names= {"-o", "--output"}, description = "The output file for generated artifacts.json file", defaultValue = "artifacts.json") Path output;

//    @Option(names = {"--interesting-artifacts"}, description = "The local file containing interesting artifacts infos", defaultValue = "interesting_artifacts.json")
//    private Path localInterestingArtifacts;

    @Option(names = {"--techempower-frameworks-local-clone"}, description = "The techempower frameworks local clone", 
    		defaultValue = "../../FrameworkBenchmarks/frameworks")
    private Path techEmpowerFrameworks;
    @Option(names = {"--searched-artifacts-cache"}, description = "List of already searched artifacts", 
    		defaultValue = "cached_artifacts.json")
    private Path cachedArtifacts;
    @Option(names = {"-u", "--server-url"}, description = "The used mvnrepository server", 
    		defaultValue = "https://mvnrepository.com") String mvnRepositoryServer;
    @Option(names = {"--generate-history"}, description ="Generate an history branch with commits for each month")
    boolean generateHistory;
    @Option(names = {"--git-folder"}, description = "The output folder where data will be written", 
    		defaultValue = "../history") Path gitHistory;
    @Option(names = {"--cache-folder"}, description = "Since fetching all artifacts could be very long, I prefer to manage a ocal cache, preventing the need for re-downloading everything", 
    		defaultValue = "../.cache")
    private Path cache;

	Gson gson = new GsonBuilder()
			.setPrettyPrinting()
			.create();

	private String artifactDetailsExtractor;


    public static void main(String... args) {
        int exitCode = new CommandLine(new ExtractPopularMvnRepositoryArtifacts()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
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
	        					new PopularArtifactLoader(this),
	        					new TechEmpowerArtifactLoader(techEmpowerFrameworks)
										));
	        	var json = gson.toJson(allArtifactInformations);
	        	FileUtils.write(cachedArtifacts.toFile(), json, "UTF-8");
        	}
        	if(generateHistory) {
        		new HistoryBuilder(mvnRepositoryServer, 
        				gitHistory, 
        				cache, 
        				output, 
        				gson, 
        				this::newPage, this::addDetails)
        		.generateHistoryFor(context, allArtifactInformations);
        	} else {
	        	Collection allDetails = obtainAllDetails(context, allArtifactInformations);
		        logger.info("Exporting artifacts to " + output.toAbsolutePath().toFile().getAbsolutePath());
		        FileUtils.write(output.toFile(), gson.toJson(allDetails), "UTF-8");
		        logger.info(String.format("Exported %d artifacts to %s", allDetails.size(), output));
        	}
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
