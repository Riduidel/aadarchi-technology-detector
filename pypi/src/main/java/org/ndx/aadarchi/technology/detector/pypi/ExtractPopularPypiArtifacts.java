package org.ndx.aadarchi.technology.detector.pypi;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.ndx.aadarchi.technology.detector.helper.ArtifactLoader;
import org.ndx.aadarchi.technology.detector.helper.ArtifactLoaderCollection;
import org.ndx.aadarchi.technology.detector.helper.BaseHistoryBuilder;
import org.ndx.aadarchi.technology.detector.helper.InterestingArtifactsDetailsDownloader;
import org.ndx.aadarchi.technology.detector.model.ArtifactDetails;

import com.github.fge.lambdas.Throwing;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "ExtractPopularPypiArtifacts", mixinStandardHelpOptions = true, version = "ExtractPopularNpmjsArtifacts 0.1")
public
class ExtractPopularPypiArtifacts extends InterestingArtifactsDetailsDownloader<PypiContext> {
	public static final Logger logger = Logger.getLogger(ExtractPopularPypiArtifacts.class.getName());

	public static void main(String... args) {
        int exitCode = new CommandLine(new ExtractPopularPypiArtifacts()).execute(args);
        System.exit(exitCode);
    }

	@Override
	public Integer call() throws Exception {
		super.doCall(new PypiContext(client));
		return 0;
	}
	
	@Override
	public Path getCache() {
		return super.getCache().toAbsolutePath().resolve("pypi");
	}
	
	@Override
	protected ArtifactLoaderCollection<PypiContext> createArtifactLoaderCollection(PypiContext context) {
		return new DetailFetchingArtifactLoaderCollection(
				getCache(),
				getArtifactLoaderCollection(context));
	}

	@Override
	protected Collection<ArtifactLoader<? super PypiContext>> getArtifactLoaderCollection(PypiContext context) {
		return Arrays.asList( 
    			// Way to much complicated
//    			new CodebaseShowArtifacts(),
				new PythonTechEmpowerArtifacts(getCache(), techEmpowerFrameworks),
    			new PopularPypiArtifacts(getCache(), client)
    			);
	}

	@Override
	protected Collection<ArtifactDetails> injectDownloadInfosFor(PypiContext context,
			Collection<ArtifactDetails> allDetails) {
			String period = "last-month";
			allDetails = getAllDownloadsForPeriod(context, allDetails, period);
			return allDetails;
	}

	private Collection<ArtifactDetails> getAllDownloadsForPeriod(PypiContext context, Collection<ArtifactDetails> artifactsToQuery,
			String period) {
		// Now get download count for last month
    	return artifactsToQuery.stream()
    		// Do not use parallel, cause the download count api is quite cautious on load and will fast put an hauld on our queries
//    		.parallel()
    		.map(Throwing.function(artifact -> new DownloadsLoader(artifact, getCache(), period).getDownloads(context)))
    		.filter(artifact -> artifact.getDownloads()>0)
			.sorted()
    		.collect(Collectors.toList());
	}

	@Override
	protected BaseHistoryBuilder<PypiContext> createHistoryBuilder() {
		throw new UnsupportedOperationException("TODO implement #createHistoryBuilder");
	}
}
