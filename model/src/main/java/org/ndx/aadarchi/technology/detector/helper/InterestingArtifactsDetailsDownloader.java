package org.ndx.aadarchi.technology.detector.helper;

import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.kohsuke.github.GHRateLimit.Record;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.kohsuke.github.RateLimitChecker;
import org.ndx.aadarchi.technology.detector.augmenters.Augmenters;
import org.ndx.aadarchi.technology.detector.exceptions.CannottReadFromCache;
import org.ndx.aadarchi.technology.detector.exceptions.CannotWriteToCache;
import org.ndx.aadarchi.technology.detector.history.BaseHistoryBuilder;
import org.ndx.aadarchi.technology.detector.loader.ArtifactLoader;
import org.ndx.aadarchi.technology.detector.loader.ArtifactLoaderCollection;
import org.ndx.aadarchi.technology.detector.loader.ExtractionContext;
import org.ndx.aadarchi.technology.detector.mappers.Mappers;
import org.ndx.aadarchi.technology.detector.mappers.MappingGenerator;
import org.ndx.aadarchi.technology.detector.model.ArtifactDetails;

import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.SimpleType;
import com.fasterxml.jackson.module.jsonSchema.jakarta.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.jakarta.JsonSchemaGenerator;

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
	@Option(names = { "--force-rebuild-history" }, description = "When set to true, the history branch will be deleted and rebuilt from scratch")
	public boolean forceRebuildHistory;
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
	@Option(names = { "-s",
		"--schema" }, description = "The output file for generated JSON schema file", defaultValue = "schema.json")
	protected Path schema;
	@Option(names = {
			"--techempower-frameworks-local-clone" }, description = "The techempower frameworks local clone"
					, defaultValue = "../../FrameworkBenchmarks/frameworks")
	protected Path techEmpowerFrameworks;
	@Option(names = { "--github-token" }) protected String githubToken;

	/**
	 * Perform all artifact analysis by 
	 * <ul>
	 * <li>Loading a list of artifacts ({@link #searchInterestingArtifacts(ExtractionContext)})
	 * <li>If history generate is required, calls {@link #generateHistoryOf(ExtractionContext, Collection)}
	 * <li>If no history generation is needed
	 * <ul>
	 * <li>Get download count with {@link #injectDownloadInfosFor(ExtractionContext, Collection, LocalDate)}
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
					artifactDetails = FileHelper.readFromFile(output.toFile(), ArtifactDetails.LIST);
				} catch (IOException e) {
					throw new CannottReadFromCache("Failed to read artifact details from file", e);
				}
			} else {
				LocalDate firstDayOfMonth = LocalDate.now()
						.withDayOfMonth(1);
		    	artifactDetails = injectDownloadInfosFor(context, interestingArtifacts, firstDayOfMonth);
				artifactDetails = Augmenters.augmentArtifacts(context, artifactDetails, firstDayOfMonth);
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
			JsonSchemaGenerator generator = new JsonSchemaGenerator(FileHelper.getObjectMapper());
			JsonSchema jsonSchema = generator.generateSchema(CollectionType.construct(artifactDetails.getClass(), SimpleType.construct(ArtifactDetails.class)));
			FileUtils.write(schema.toFile(), 
					FileHelper.getObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(jsonSchema), 
					"UTF-8");
			FileHelper.writeToFile(artifactDetails, output.toFile());
		} catch (IOException e) {
			throw new CantWriteArtifacts("Failed to write artifact details to file",e);
		}
	}

	protected abstract Collection<ArtifactDetails> injectDownloadInfosFor(Context context, Collection<ArtifactDetails> interestingArtifacts, LocalDate date);

	protected void generateHistoryOf(Context context, Collection<ArtifactDetails> artifacts) {
		try {
			createHistoryBuilder()
				.generateHistoryFor(context, artifacts);
		} catch(IOException | GitAPIException e) {
			throw new CantGenerateHistory("Failed to generate history", e);
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
			throw new CannotWriteToCache("Unable to write into cache", e);
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
	
	private static class ProgressingRateLimitChecker extends RateLimitChecker {
		@Override
		protected boolean checkRateLimit(Record rateLimitRecord, long count) throws InterruptedException {
			if(rateLimitRecord.getLimit()-rateLimitRecord.getRemaining()>rateLimitRecord.getLimit()-1000) {
				int delay = -1000-(rateLimitRecord.getLimit()-rateLimitRecord.getRemaining());
				logger.info(String.format("Approaching rate limit, starting progressive slow down (waiting %d s.)", delay));
				synchronized(this) {
					this.wait(delay*1000);
				}
			}
			var returned = super.checkRateLimit(rateLimitRecord, count);
			if(returned)
				logger.warning("Rate limit is reached ("+rateLimitRecord+")");
			return returned;
		}
	}


	protected GitHub getGithub() {
		try {
			return new GitHubBuilder()
					.withRateLimitChecker(new ProgressingRateLimitChecker())
					.withJwtToken(githubToken).build();
		} catch (IOException e) {
			throw new CannotInitializeGitHubClient("Unable to connect to GitHub", e);
		}
	}
}
