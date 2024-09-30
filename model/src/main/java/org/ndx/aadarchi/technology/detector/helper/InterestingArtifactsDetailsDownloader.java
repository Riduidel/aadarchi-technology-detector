package org.ndx.aadarchi.technology.detector.helper;

import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.ndx.aadarchi.technology.detector.history.BaseHistoryBuilder;
import org.ndx.aadarchi.technology.detector.loader.ArtifactLoader;
import org.ndx.aadarchi.technology.detector.loader.ArtifactLoaderCollection;
import org.ndx.aadarchi.technology.detector.loader.ExtractionContext;
import org.ndx.aadarchi.technology.detector.mappers.Mappers;
import org.ndx.aadarchi.technology.detector.mappers.MappingGenerator;
import org.ndx.aadarchi.technology.detector.model.ArtifactDetails;

import picocli.CommandLine.Option;

/**
 * Class defining a common (and I hope efficient) method to obtain artifacts for various languages
 * 
 * Main method of this class is {@link #doCall(ExtractionContext)} which executes all code
 */
public abstract class InterestingArtifactsDetailsDownloader<Context extends ExtractionContext> implements Callable<Integer>{
	public static final Logger logger = Logger.getLogger(InterestingArtifactsDetailsDownloader.class.getName());
	public static HttpClient client = HttpClient.newHttpClient();
	@Option(names = { "--generate-history" }, description = "Generate an history branch with commits for each month")
	public boolean generateHistory;
	@Option(names = { "--generate-mapping-files" }, description = "Generate mapping files for various data analysis. This will typically be invoked during development.")
	public boolean generateMappingFiles;
	@Option(names = {"--cache-folder" }, 
			description = "Since fetching all artifacts could be very long, I prefer to manage a ocal cache, preventing the need for re-downloading everything", 
			defaultValue = "../.cache")
	private Path cache;
	@Option(names = {"--resource-folder" }, 
			description = "Resource folder of the given generator (used ONLY when --generate-mapping-files is set)", 
			defaultValue = "src/main/resources")
	private Path resources;
	@Option(names = {
			"--git-folder" }, description = "The output folder where data will be written")
	protected Path gitHistory;
	@Option(names = { "-o",
			"--output" }, description = "The output file for generated artifacts.json file", defaultValue = "artifacts.json")
	protected Path output;
	@Option(names = {
			"--techempower-frameworks-local-clone" }, description = "The techempower frameworks local clone"
					, defaultValue = "../../FrameworkBenchmarks/frameworks")
	protected Path techEmpowerFrameworks;

	/**
	 * Perform all artifact analysis by 
	 * <ul>
	 * <li>Loading a list of artifacts ({@link #searchInterestingArtifacts(ExtractionContext)})
	 * <li>If history generate is required, calls {@link #generateHistoryOf(ExtractionContext, Collection)}
	 * <li>If no history generation is needed
	 * <ul>
	 * <li>Get download count with {@link #injectDownloadInfosFor(ExtractionContext, Collection)}
	 * <li>Write results to file using {@link #writeDetails(Collection)}
	 * </ul>
	 * </ul>
	 * @param context
	 */
	protected void doCall(Context context) {
		Collection<ArtifactDetails> interestingArtifacts = searchInterestingArtifacts(context);
		// Changing this period will allow us to build very fast a very good history
		if(generateHistory) {
			generateHistoryOf(context, interestingArtifacts);
		} else {
	    	Collection<ArtifactDetails> artifactDetails;
			if(output.toFile().exists() && output.toFile().lastModified()>System.currentTimeMillis()-60*60*1000*24) {
				try {
					artifactDetails = FileHelper.readFromFile(output.toFile());
				} catch (IOException e) {
					throw new RuntimeException("Can't read file", e);
				}
			} else {
		    	artifactDetails = injectDownloadInfosFor(context, interestingArtifacts);
		    	writeDetails(artifactDetails);
			}
	    	if(generateMappingFiles) {
	    		generateMappingFiles(artifactDetails);
	    	}
		}
	}

	
	private void generateMappingFiles(Collection<ArtifactDetails> artifactDetails) {
		for(MappingGenerator generator : Mappers.getMappers()) {
			generator.generateMapping(artifactDetails, resources);
		}
	}

	private void writeDetails(Collection<ArtifactDetails> artifactDetails) {
		try {
			FileHelper.writeToFile(artifactDetails, output.toFile());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	protected abstract Collection<ArtifactDetails> injectDownloadInfosFor(Context context, Collection<ArtifactDetails> interestingArtifacts);

	protected void generateHistoryOf(Context context, Collection<ArtifactDetails> artifacts) {
		try {
			createHistoryBuilder()
				.generateHistoryFor(context, artifacts);
		} catch(IOException | GitAPIException e) {
			throw new RuntimeException(e);
		}
		
	}

	protected abstract BaseHistoryBuilder<Context> createHistoryBuilder();

	/**
	 * Produces a list of interesting artifacts for this ecosystem.
	 * This method will typically scan "Top 100", TechEmpower frameworks, or other well-known sources 
	 * @param context TODO
	 * @return a list of **incomplete** ArtifactDetails (level of completion depends upon the implementation
	 */
	protected Collection<ArtifactDetails> searchInterestingArtifacts(Context context) {
		ArtifactLoaderCollection<Context> loader = createArtifactLoaderCollection(context);
		try {
			return loader.loadArtifacts(context);
		} catch(Exception e) {
			throw new RuntimeException("Unable to write into cache", e);
		}
	}

	protected ArtifactLoaderCollection<Context> createArtifactLoaderCollection(Context context) {
		return new ArtifactLoaderCollection<Context>(
				getCache(),
				getArtifactLoaderCollection(context)
				);
	}

	protected abstract Collection<ArtifactLoader<Context>> getArtifactLoaderCollection(Context context);

	public Path getCache() {
		return cache;
	}
}
