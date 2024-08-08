package org.ndx.aadarchi.technology.detector.mvnrepository;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import com.microsoft.playwright.Page;

/**
 * Load interesting artifacts from a local file
 */
public class LocalFileArtifactLoader extends ArtifactLoader {
	/**
	 * 
	 */
	private final ExtractPopularMvnRepositoryArtifacts extractPopularMvnRepositoryArtifacts;
	private Path referenceFile;

	public LocalFileArtifactLoader(ExtractPopularMvnRepositoryArtifacts extractPopularMvnRepositoryArtifacts, Path file) {
		this.extractPopularMvnRepositoryArtifacts = extractPopularMvnRepositoryArtifacts;
		this.referenceFile = file;
	}

	@Override
	public Set<ArtifactInformations> loadArtifacts(Page page) throws IOException {
		// Read the reference file
		var text = FileUtils.readFileToString(referenceFile.toFile(), "UTF-8");
		List<Map<String, String>> entries = this.extractPopularMvnRepositoryArtifacts.gson.fromJson(text, List.class);
		Set<ArtifactInformations> returned = new HashSet<ArtifactInformations>();
		entries.forEach(artifact -> returned.addAll(getArtifactInformations(page, artifact)));
		return returned;
	}

	private Collection<ArtifactInformations> getArtifactInformations(Page page, Map<String, String> artifact) {
		Set<ArtifactInformations> returned = new HashSet<ArtifactInformations>();
		var groupId = artifact.get("groupId");
		if(artifact.containsKey("artifactId")) {
			returned.add(new ArtifactInformations(groupId, artifact.get("artifactId")));
		} else {
			returned.addAll(loadAllArtifactsOfGroup(page, groupId));
		}
		return returned;
	}

	private Collection<? extends ArtifactInformations> loadAllArtifactsOfGroup(Page page, String groupId) {
		return loadPageList(page, this.extractPopularMvnRepositoryArtifacts.mvnRepositoryServer+"/artifact/"+groupId);
	}
}