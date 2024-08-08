package org.ndx.aadarchi.technology.detector.npmjs;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.ndx.aadarchi.technology.detector.model.ArtifactDetails;
import org.ndx.aadarchi.technology.detector.model.ArtifactDetailsBuilder;

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
	static final Logger logger = Logger.getLogger(ExtractPopularNpmjsArtifacts.class.getName());
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
    		defaultValue = "../.cache/npmjs") Path cache;
    @Option(names = {"--git-folder"}, description = "The output folder where data will be written", 
    		defaultValue = "../history") Path gitHistory;

    private Set<ArtifactDetails> fetchArtifactInformations(
			List<ArtifactLoader> sources) {
		return sources.stream()
			.map(Throwing.function(source -> source.loadArtifacts()))
			.flatMap(artifactInformationsSet -> artifactInformationsSet.stream())
			.peek(artifactInformations -> logger.info(String.format("found artifact %s", artifactInformations)))
			.sorted(Comparator.comparing((ArtifactDetails a) -> a.name))
			.collect(Collectors.toSet());
	}

	public static void main(String... args) {
        int exitCode = new CommandLine(new ExtractPopularNpmjsArtifacts()).execute(args);
        System.exit(exitCode);
    }
	
	@Override
    public Integer call() throws Exception {
    	Collection<ArtifactDetails> allDetails = fetchArtifactInformations(Arrays.asList(
    			// Way to much complicated
//    			new CodebaseShowArtifacts(),
    			new PopularNpmArtifacts(cache, client)));
    	// Changing this period will allow us to build very fast a very good history
    	if(generateHistory) {
    		new HistoryBuilder(gitHistory, cache, Throwing.biFunction(this::getAllDownloadsForPeriod)).generateHistoryFor(allDetails);
    		
    	} else {
	    	String period = "last-month";
	    	allDetails = getAllDownloadsForPeriod(allDetails, period);
	        writeArtifacts(allDetails, output.toFile());
    	}
        return 0;
    }

    static List<ArtifactDetails> readArtifacts(File file) throws IOException {
    	return gson.fromJson(FileUtils.readFileToString(file, "UTF-8"),
				new TypeToken<List<ArtifactDetails>>() {});
    }
	static void writeArtifacts(Collection<ArtifactDetails> allDetails, File file) throws IOException {
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
	private Collection<ArtifactDetails> getAllDownloadsForPeriod(Collection<ArtifactDetails> artifactsToQuery, String period) throws IOException {
		// Now get download count for last month
    	return artifactsToQuery.stream()
    		// Do not use parallel, cause the download count api is quite cautious on load and will fast put an hauld on our queries
//    		.parallel()
    		.map(Throwing.function(artifact -> countDownloadsOf(artifact, period)))
    		.filter(artifact -> artifact.downloads>0)
			.sorted(Comparator.comparing((ArtifactDetails a) -> a.name))
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
	private ArtifactDetails countDownloadsOf(ArtifactDetails artifact, String period) throws IOException, InterruptedException {
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
				return ArtifactDetailsBuilder.toBuilder(artifact)
				.downloads(jsonResponse.downloads)
				.build();
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
