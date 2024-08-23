package org.ndx.aadarchi.technology.detector.mvnrepository;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.beanutils.BeanUtils;
import org.ndx.aadarchi.technology.detector.helper.ArtifactLoader;
import org.ndx.aadarchi.technology.detector.helper.InterestingArtifactsDetailsDownloader;
import org.ndx.aadarchi.technology.detector.helper.TechEmpowerArtifactLoader;
import org.ndx.aadarchi.technology.detector.model.ArtifactDetails;
import org.ndx.aadarchi.technology.detector.model.ArtifactDetailsBuilder;

import com.github.fge.lambdas.Throwing;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Browser.NewContextOptions;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
@Command(name = "ExtractPuploarMvnRepositoryArtifacts", mixinStandardHelpOptions = true, version = "ExtractPuploarMvnRepositoryArtifacts 0.1",
        description = "ExtractPuploarMvnRepositoryArtifacts made with jbang")
class ExtractPopularMvnRepositoryArtifacts extends InterestingArtifactsDetailsDownloader<MvnContext> implements Callable<Integer> {
	public static final Logger logger = Logger.getLogger(ExtractPopularMvnRepositoryArtifacts.class.getName());
	
    @Option(names = {"--local-artifacts"}, description = "A list of locally defined artifacts of interest.\n"
    		+ "This typically allows us to add frameworks of interests that cannot be found otherwise", 
    		defaultValue = "local_artifacts.json")
    private Path localInterestingArtifacts;
    @Option(names = {"-u", "--server-url"}, description = "The used mvnrepository server", 
    		defaultValue = "https://mvnrepository.com") String mvnRepositoryServer;

    public static void main(String... args) {
        int exitCode = new CommandLine(new ExtractPopularMvnRepositoryArtifacts()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        try (Playwright playwright = Playwright.create()) {
        	MvnContext mvnContext = new MvnContext(createPlaywrightContext(playwright));
        	super.doCall(mvnContext);
        }
        return 0;
    }

	@Override
	protected HistoryBuilder createHistoryBuilder() {
		return new HistoryBuilder(mvnRepositoryServer, gitHistory, getCache(), output);
	}

	private List<ArtifactDetails> findRelocated(Map artifactInformations) {
    	if(artifactInformations.containsKey("relocation")) {
    		Map relocation = (Map) artifactInformations.get("relocation");
    		Map<String, String> page = (Map<String, String>) relocation.get("page");
    		var groupId = page.get("groupId");
    		var artifactId = page.get("artifactId");
    		var relocated = ArtifactDetailsBuilder
    				.artifactDetails()
    					.coordinates(String.format("%s:%s", groupId, artifactId))
    					.build();
    		return List.of(relocated);
    	} else {
    		return List.of();
    	}
    }


    @Override
	protected Collection<ArtifactDetails> injectDownloadInfosFor(MvnContext mvnContext, Collection<ArtifactDetails> allArtifactInformations) {
		return (Collection<ArtifactDetails>) obtainAllDetails(mvnContext, allArtifactInformations);
	}

    private Collection<ArtifactDetails> obtainAllDetails(MvnContext context,
			Collection<ArtifactDetails> allArtifactInformations) {
		Map<ArtifactDetails, ArtifactDetails> resolvedArtifacts = new TreeMap<ArtifactDetails, ArtifactDetails>();
		while(!allArtifactInformations.isEmpty()) {
			allArtifactInformations = allArtifactInformations.stream()
				.flatMap(artifactInformations -> {
					Optional<Map> details = context.addDetails(ArtifactDetailsUtils.getArtifactUrl(artifactInformations, mvnRepositoryServer));
					details.stream()
						.forEach(d -> {
							try {
								ArtifactDetails updated = ArtifactDetailsBuilder.toBuilder(artifactInformations).build();
								BeanUtils.populate(updated, d);
								resolvedArtifacts.put(artifactInformations, updated);
							} catch(InvocationTargetException | IllegalAccessException e) {
								throw new RuntimeException(e);
							}
						});
					return details.stream();
				})
				// Now we're extracted details, let's do some complementary filtering
				.filter(informationsMap -> informationsMap.containsKey("repositories") ? ((List) informationsMap.get("repositories")).contains("Central") : false)
				.flatMap(informationsMap -> findRelocated(informationsMap).stream())
				.filter(artifactInformations -> !resolvedArtifacts.containsKey(artifactInformations))
				.collect(Collectors.toList());
		}
		return resolvedArtifacts.values();
	}

	private List<ArtifactDetails> fetchArtifactInformations(
			MvnContext context, List<ArtifactLoader<MvnContext>> sources) {
		return sources.stream()
			.map(Throwing.function(source -> source.loadArtifacts(context)))
			.flatMap(artifactInformationsSet -> artifactInformationsSet.stream())
			.peek(artifactInformations -> logger.info(String.format("found artifact %s", artifactInformations)))
			.sorted()
			.collect(Collectors.toList());
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
	
	@Override
	public Path getCache() {
		return super.getCache().toAbsolutePath().resolve("mvnrepository");
	}

	@Override
	protected Collection<ArtifactLoader<Context>> getArtifactLoaderCollection(MvnContext context) {
		return Arrays.asList(
		new LocalFileArtifactLoader(getCache(), localInterestingArtifacts, mvnRepositoryServer),
		new PopularArtifactLoader(getCache(), mvnRepositoryServer),
		new JavaTechEmpowerArtifactLoader(getCache(),
				techEmpowerFrameworks)
		);
	}
}
