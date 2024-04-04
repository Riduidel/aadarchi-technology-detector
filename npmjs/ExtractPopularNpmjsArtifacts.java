///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS info.picocli:picocli:4.6.3

//DEPS com.google.code.gson:gson:2.10.1
//DEPS commons-io:commons-io:2.15.0
//DEPS com.github.fge:throwing-lambdas:0.5.0
//DEPS org.apache.commons:commons-lang3:3.13.0
//DEPS org.eclipse.jgit:org.eclipse.jgit:6.7.0.202309050840-r


import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;

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
	private static final String DOWNLOADS_LAST_MONTH = "https://api.npmjs.org/downloads/point/%s/%s";
	HttpClient client = HttpClient.newHttpClient();
	private Gson gson = new GsonBuilder()
			.setPrettyPrinting()
			.create();

    @Option(names= {"-o", "--output"}, description = "The output file for generated artifacts.json file", defaultValue = "artifacts.json")
    private Path output;

    public interface ArtifactLoader {

		public List<ArtifactInformations> loadArtifacts() throws Exception;
		
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
		public static final String POPULAR_ARTIFACTS = "https://registry.npmjs.com/-/v1/search?text=not:unstable&popularity=1.0&size=250";
		
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
		public List<ArtifactInformations> loadArtifacts() throws IOException, InterruptedException {
			HttpRequest request = HttpRequest.newBuilder(URI.create(POPULAR_ARTIFACTS)).build();
			HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
			NpmJsResponse jsonResponse = gson.fromJson(response.body(), new TypeToken<NpmJsResponse>() {});
			return jsonResponse.objects.stream()
					.map(a -> a.artifact)
					.collect(Collectors.toList());
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

    @Override
    public Integer call() throws Exception {
    	Collection<ArtifactInformations> allDetails = fetchArtifactInformations(Arrays.asList(
    	        // TODO add artifacts from codebaseshoow
    	        // TODO add artifacts from npm rank
    			new PopularNpmArtifacts()));
    	String period = "last-month";
    	// Now get download count for last month
    	allDetails = allDetails.stream()
    		.parallel()
    		.map(Throwing.function(artifact -> countDownloadsOf(artifact, period)))
			.sorted(Comparator.comparing((ArtifactInformations a) -> a.name))
    		.collect(Collectors.toList());
        logger.info("Exporting artifacts to " + output.toAbsolutePath().toFile().getAbsolutePath());
        FileUtils.write(output.toFile(), gson.toJson(allDetails), "UTF-8");
        logger.info(String.format("Exported %d artifacts to %s", allDetails.size(), output));
        return 0;
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
		HttpRequest request = HttpRequest.newBuilder(URI.create(
				String.format(DOWNLOADS_LAST_MONTH, period, artifact.name))).build();
		HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
		DownloadCount jsonResponse = gson.fromJson(response.body(), new TypeToken<DownloadCount>() {});
		return artifact.withDownloads(jsonResponse);
	}
}
