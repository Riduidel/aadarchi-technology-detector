package org.ndx.aadarchi.technology.detector.packagist;

import org.ndx.aadarchi.technology.detector.helper.InterestingArtifactsDetailsDownloader;
import org.ndx.aadarchi.technology.detector.history.BaseHistoryBuilder;
import org.ndx.aadarchi.technology.detector.loader.ArtifactLoader;
import org.ndx.aadarchi.technology.detector.loader.ArtifactLoaderCollection;
import org.ndx.aadarchi.technology.detector.loader.DetailFetchingArtifactLoaderCollection;
import org.ndx.aadarchi.technology.detector.model.ArtifactDetails;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;

@CommandLine.Command(name = "ExtractPopularPackagistrtifacts", mixinStandardHelpOptions = true, version = "ExtractPopularPackagistrtifacts 0.1",
        description = "ExtractPopularPackagistrtifacts made with jbang")
public class ExtractPopularPackagistrtifacts extends InterestingArtifactsDetailsDownloader<PackagistContext> {

    public static void main(String... args) {
        int exitCode = new CommandLine(new ExtractPopularPackagistrtifacts()).execute(args);
        System.exit(exitCode);
    }

    @Override
    protected Collection<ArtifactDetails> injectDownloadInfosFor(PackagistContext context, Collection<ArtifactDetails> interestingArtifacts, LocalDate date) {
        try {
            interestingArtifacts = context.getAllDownloadsForLastMonth(interestingArtifacts);
            return interestingArtifacts;
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected BaseHistoryBuilder<PackagistContext> createHistoryBuilder() {
        return null;
    }

    @Override
    protected Collection<ArtifactLoader<PackagistContext>> getArtifactLoaderCollection(PackagistContext context) {
        return Arrays.asList(
                new PhpTechEmpowerArtifacts(getCache(), techEmpowerFrameworks),
                new PopularPackagistArtifacts(getCache(), client)
        );
    }

    @Override
    public Integer call() throws Exception {
        super.doCall(new PackagistContext(client, getCache(), getGithub()));
        return 0;
    }


    @Override
    public Path getCache() {
        return super.getCache().toAbsolutePath().resolve("packagist");
    }

    @Override
    protected ArtifactLoaderCollection<PackagistContext> createArtifactLoaderCollection(PackagistContext context) {
        return new DetailFetchingArtifactLoaderCollection<PackagistContext>(getCache(), getArtifactLoaderCollection(context));
    }
}
