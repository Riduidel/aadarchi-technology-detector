package org.ndx.aadarchi.technology.detector.npmjs;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Logger;

import org.ndx.aadarchi.technology.detector.helper.ArtifactLoader;
import org.ndx.aadarchi.technology.detector.helper.ArtifactLoaderCollection;
import org.ndx.aadarchi.technology.detector.helper.DetailFetchingArtifactLoaderCollection;
import org.ndx.aadarchi.technology.detector.helper.InterestingArtifactsDetailsDownloader;
import org.ndx.aadarchi.technology.detector.model.ArtifactDetails;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "ExtractPopularNpmjsArtifacts", mixinStandardHelpOptions = true, version = "ExtractPopularNpmjsArtifacts 0.1",
        description = "ExtractPopularNpmjsArtifacts made with jbang")
public
class ExtractPopularNpmjsArtifacts extends InterestingArtifactsDetailsDownloader<NpmjsContext> {
	public static final Logger logger = Logger.getLogger(ExtractPopularNpmjsArtifacts.class.getName());

	public static void main(String... args) {
        int exitCode = new CommandLine(new ExtractPopularNpmjsArtifacts()).execute(args);
        System.exit(exitCode);
    }

	@Override
	public Integer call() throws Exception {
		super.doCall(new NpmjsContext(client));
		return 0;
	}
	

	@Override
	protected Collection<ArtifactDetails> injectDownloadInfosFor(NpmjsContext context, Collection<ArtifactDetails> allDetails) {
		try {
			String period = "last-month";
			allDetails = context.getAllDownloadsForPeriod(allDetails, period);
			return allDetails;
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected HistoryBuilder createHistoryBuilder() {
		return new HistoryBuilder(this, gitHistory, getCache());
	}

	
	@Override
	public Path getCache() {
		return super.getCache().toAbsolutePath().resolve("npmjs");
	}

	@Override
	protected Collection<ArtifactLoader<NpmjsContext>> getArtifactLoaderCollection(NpmjsContext context) {
		return Arrays.asList( 
    			// Way to much complicated
//    			new CodebaseShowArtifacts(),
				new JavascriptTechEmpowerArtifacts(getCache(), techEmpowerFrameworks),
    			new PopularNpmArtifacts(getCache(), client)
    			);
	}
	
	@Override
	protected ArtifactLoaderCollection<NpmjsContext> createArtifactLoaderCollection(NpmjsContext context) {
		return new DetailFetchingArtifactLoaderCollection<NpmjsContext>(getCache(), getArtifactLoaderCollection(context));
	}
}
