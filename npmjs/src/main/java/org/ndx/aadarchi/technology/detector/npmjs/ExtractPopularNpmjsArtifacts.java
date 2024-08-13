package org.ndx.aadarchi.technology.detector.npmjs;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.ndx.aadarchi.technology.detector.helper.FileHelper;
import org.ndx.aadarchi.technology.detector.helper.InterestingArtifactsDetailsDownloader;
import org.ndx.aadarchi.technology.detector.helper.NoContext;
import org.ndx.aadarchi.technology.detector.model.ArtifactDetails;
import org.ndx.aadarchi.technology.detector.model.ArtifactDetailsBuilder;

import com.github.fge.lambdas.Throwing;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "ExtractPopularNpmjsArtifacts", mixinStandardHelpOptions = true, version = "ExtractPopularNpmjsArtifacts 0.1",
        description = "ExtractPopularNpmjsArtifacts made with jbang")
public
class ExtractPopularNpmjsArtifacts extends InterestingArtifactsDetailsDownloader<NoContext> {
	public static final Logger logger = Logger.getLogger(ExtractPopularNpmjsArtifacts.class.getName());
	/**
	 * @see https://github.com/npm/registry/blob/main/docs/download-counts.md
	 */
	private static final String DOWNLOADS_LAST_MONTH = "https://api.npmjs.org/downloads/point/%s/%s";
	private Set<ArtifactDetails> fetchArtifactInformations(
			List<ArtifactLoader> sources) {
		return sources.stream()
			.map(Throwing.function(source -> source.loadArtifacts()))
			.flatMap(artifactInformationsSet -> artifactInformationsSet.stream())
			.peek(artifactInformations -> logger.info(String.format("found artifact %s", artifactInformations)))
			.sorted(Comparator.comparing(ArtifactDetails::getName))
			.collect(Collectors.toSet());
	}

	public static void main(String... args) {
        int exitCode = new CommandLine(new ExtractPopularNpmjsArtifacts()).execute(args);
        System.exit(exitCode);
    }

	@Override
	public Integer call() throws Exception {
		super.doCall(new NoContext());
		return 0;
	}
	

	@Override
	protected Collection<ArtifactDetails> injectDownloadInfosFor(NoContext context, Collection<ArtifactDetails> allDetails) {
		try {
			String period = "last-month";
			allDetails = getAllDownloadsForPeriod(allDetails, period);
			return allDetails;
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected void generateHistoryOf(NoContext context, Collection<ArtifactDetails> allDetails) {
		try {
			new HistoryBuilder(gitHistory, cache)
				.generateHistoryFor(context, allDetails);
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}

	public Set<ArtifactDetails> searchInterestingArtifacts(NoContext context) {
		return fetchArtifactInformations(Arrays.asList(
    			// Way to much complicated
//    			new CodebaseShowArtifacts(),
    			new PopularNpmArtifacts(cache, client)));
	}

    /**
	 * Get all downloads on a given period, as defined by npmjs download api
	 * @param artifactsToQuery
	 * @param period the period of time to get downloads for
	 * @return list of artifacts having at least one download on this period
	 * @throws IOException
	 */
	static Collection<ArtifactDetails> getAllDownloadsForPeriod(Collection<ArtifactDetails> artifactsToQuery, String period) throws IOException {
		// Now get download count for last month
    	return artifactsToQuery.stream()
    		// Do not use parallel, cause the download count api is quite cautious on load and will fast put an hauld on our queries
//    		.parallel()
    		.map(Throwing.function(artifact -> countDownloadsOf(artifact, period)))
    		.filter(artifact -> artifact.getDownloads()>0)
			.sorted(Comparator.comparing(ArtifactDetails::getName))
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
	private static ArtifactDetails countDownloadsOf(ArtifactDetails artifact, String period) throws IOException, InterruptedException {
		logger.info(String.format("Getting downloads counts of %s for period %s", 
				artifact.getName(), period));
		int status = 1000;
		do {
			HttpRequest request = HttpRequest.newBuilder(URI.create(
					String.format(DOWNLOADS_LAST_MONTH, period, artifact.getName()))).build();
			try {
				HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
				status = response.statusCode();
				DownloadCount jsonResponse = FileHelper.gson.fromJson(response.body(), new TypeToken<DownloadCount>() {});
				return ArtifactDetailsBuilder.toBuilder(artifact)
				.downloads(jsonResponse.downloads)
				.build();
			} catch(Exception e) {
				logger.log(Level.WARNING, "Seems like we were hit by an error. Let's wait some time and retry latter ...");
				synchronized(artifact) {
					artifact.wait(5*60*1000);
				}
			}
		} while(status>=400);
		return artifact;
	}
}
