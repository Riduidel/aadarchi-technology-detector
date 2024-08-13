package org.ndx.aadarchi.technology.detector.helper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.ndx.aadarchi.technology.detector.model.ArtifactDetails;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import picocli.CommandLine.Option;

/**
 * Class defining a common (and I hope efficient) method to obtain artifacts for various languages
 */
public abstract class InterestingArtifactsDetailsDownloader<Context extends ExtractionContext> implements Callable<Integer>{
	public static final Logger logger = Logger.getLogger(InterestingArtifactsDetailsDownloader.class.getName());
	public static Gson gson = new GsonBuilder()
				.setPrettyPrinting()
				.create();

	public static List<ArtifactDetails> readFromFile(File file) throws IOException {
		return gson.fromJson(FileUtils.readFileToString(file, "UTF-8"),
				new TypeToken<List<ArtifactDetails>>() {});
	}

	public static void writeToFile(Collection<ArtifactDetails> allDetails, File file) throws IOException {
		logger.info("Exporting artifacts to " + file.getAbsolutePath());
		FileUtils.write(file, gson.toJson(allDetails), "UTF-8");
		logger.info(String.format("Exported %d artifacts to %s", allDetails.size(), file));
	}

	@Option(names = { "--generate-history" }, description = "Generate an history branch with commits for each month")
	public boolean generateHistory;
	@Option(names = {
			"--cache-folder" }, description = "Since fetching all artifacts could be very long, I prefer to manage a ocal cache, preventing the need for re-downloading everything", defaultValue = "../.cache/npmjs")
	protected Path cache;
	@Option(names = {
			"--git-folder" }, description = "The output folder where data will be written", defaultValue = "../history")
	protected Path gitHistory;
	@Option(names = { "-o",
			"--output" }, description = "The output file for generated artifacts.json file", defaultValue = "artifacts.json")
	protected Path output;

	protected void doCall(Context context) {
		Collection<ArtifactDetails> interestingArtifacts = searchInterestingArtifacts(context);
		// Changing this period will allow us to build very fast a very good history
		if(generateHistory) {
			generateHistoryOf(context, interestingArtifacts);
		} else {
	    	Collection<ArtifactDetails> artifactDetails = injectDownloadInfosFor(context, interestingArtifacts);
	    	try {
				writeToFile(artifactDetails, output.toFile());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	protected abstract Collection<ArtifactDetails> injectDownloadInfosFor(Context context, Collection<ArtifactDetails> interestingArtifacts);

	protected abstract void generateHistoryOf(Context context, Collection<ArtifactDetails> interestingArtifacts);

	/**
	 * Produces a list of interesting artifacts for this ecosystem.
	 * This method will typically scan "Top 100", TechEmpower frameworks, or other well-known sources 
	 * @param context TODO
	 * @return a list of **incomplete** ArtifactDetails (level of completion depends upon the implementation
	 */
	protected abstract Collection<ArtifactDetails> searchInterestingArtifacts(Context context);
}
