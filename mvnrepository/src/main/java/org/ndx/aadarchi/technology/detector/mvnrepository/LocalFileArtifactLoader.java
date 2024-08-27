package org.ndx.aadarchi.technology.detector.mvnrepository;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.maven.artifact.Artifact;
import org.ndx.aadarchi.technology.detector.helper.BasicArtifactLoader;
import org.ndx.aadarchi.technology.detector.helper.FileHelper;
import org.ndx.aadarchi.technology.detector.model.ArtifactDetails;
import org.ndx.aadarchi.technology.detector.model.ArtifactDetailsBuilder;

import com.microsoft.playwright.Page;

/**
 * Load interesting artifacts from a local file
 */
public class LocalFileArtifactLoader extends BasicArtifactLoader<MvnContext> {
	public static final Logger logger = Logger.getLogger(LocalFileArtifactLoader.class.getName());
	private Path referenceFile;
	private String mvnRepositoryServer;

	public LocalFileArtifactLoader(Path cache, Path file, String mvnRepositoryServer) {
		this.referenceFile = file;
		registerCachedArtifacts(cache, "local_artifacts.json");
		this.mvnRepositoryServer = mvnRepositoryServer;
	}

	private Collection<ArtifactDetails> getArtifactInformations(Page page, ArtifactDetails artifactFuzzyDetails) {
		Set<ArtifactDetails> returned = new HashSet<>();
		if(artifactFuzzyDetails.getGroupId()!=null) {
			var groupId = artifactFuzzyDetails.getGroupId();
			if(artifactFuzzyDetails.getArtifactId()==null) {
				returned.addAll(loadAllArtifactsOfGroup(page, groupId));
			} else {
				returned.add(artifactFuzzyDetails);
			}
		}
		return returned;
	}

	private Collection<? extends ArtifactDetails> loadAllArtifactsOfGroup(Page page, String groupId) {
		return MvnArtifactLoaderHelper.loadPageList(page, this.mvnRepositoryServer+"/artifact/"+groupId);
	}

	@Override
	public Collection<ArtifactDetails> doLoadArtifacts(MvnContext context) throws Exception {
		Set<ArtifactDetails> returned = new HashSet<ArtifactDetails>();
		// Read the reference file
		if(referenceFile.toFile().exists()) {
			List<ArtifactDetails> entries = FileHelper.readFromFile(referenceFile.toFile());
			Page page = context.newPage();
			entries.forEach(artifact -> returned.addAll(getArtifactInformations(page, artifact)));
		} else {
			logger.warning(() -> String.format("The reference file %s was not found, so no local artifacts will be added", referenceFile));
		}
		return returned;

	}
}