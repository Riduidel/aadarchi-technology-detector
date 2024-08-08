package org.ndx.aadarchi.technology.detector.mvnrepository;

import java.io.IOException;
import java.util.Set;

import com.microsoft.playwright.Page;

/**
 * Load popular mvnrepository artifacts by browsing the popular pages
 */
public class PopularArtifactLoader extends ArtifactLoader {

	/**
	 * 
	 */
	private final ExtractPopularMvnRepositoryArtifacts extractPopularMvnRepositoryArtifacts;

	/**
	 * @param extractPopularMvnRepositoryArtifacts
	 */
	PopularArtifactLoader(ExtractPopularMvnRepositoryArtifacts extractPopularMvnRepositoryArtifacts) {
		this.extractPopularMvnRepositoryArtifacts = extractPopularMvnRepositoryArtifacts;
	}

	@Override
	public Set<ArtifactInformations> loadArtifacts(Page page)
			throws IOException {
		return loadPageList(page, String.format("%s/popular", this.extractPopularMvnRepositoryArtifacts.mvnRepositoryServer));
	}
	
}