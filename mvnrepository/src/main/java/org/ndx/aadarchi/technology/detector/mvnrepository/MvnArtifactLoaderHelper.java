package org.ndx.aadarchi.technology.detector.mvnrepository;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.io.IOUtils;
import org.ndx.aadarchi.technology.detector.helper.ArtifactLoader;
import org.ndx.aadarchi.technology.detector.model.ArtifactDetails;
import org.ndx.aadarchi.technology.detector.model.ArtifactDetailsBuilder;

import com.microsoft.playwright.Page;

/**
 * An helper class allowing artifact loaders to navigate the mvnrepository site to get correct informations
 */
public class MvnArtifactLoaderHelper {
	private static String artifactsUrlExtractor;
	
	static {
		try {
			artifactsUrlExtractor = IOUtils.toString(MvnArtifactLoaderHelper.class.getClassLoader().getResourceAsStream("artifacts_urls_extractor.js"), "UTF-8");
		} catch (IOException e) {
			throw new UnsupportedOperationException(String.format("Unable to load script from %s", 
					ArtifactDetails.class.getClassLoader().getResource("artifacts_urls_extractor.js")), e);
		}
	}

	protected static Set<ArtifactDetails> loadPageList(Page page, String url) {
		Set<ArtifactDetails> returned = new TreeSet<ArtifactDetails>();
		while(url!=null) {
			ExtractPopularMvnRepositoryArtifacts.logger.info(String.format("Loading page %s", url));
			page.navigate(url);
			Map pageInfos = ((Map) page.evaluate(artifactsUrlExtractor));

            if (!pageInfos.containsKey("data")) {
                ExtractPopularMvnRepositoryArtifacts.logger.warning(String.format("Page %s has empty data %s", url, pageInfos));
            } else {
            	List<Map<String, String>> data = (List) pageInfos.get("data");
            	for (Map<String, String> object : data) {
					returned.add(
							ArtifactDetailsBuilder.artifactDetails()
								.groupId(object.get("groupId"))
								.artifactId(object.get("artifactId"))
								.build()
								);
				}
            }
            url = null;
            if(pageInfos.containsKey("page")) {
            	Map<String, String> paging = (Map<String, String>) pageInfos.get("page");
            	if(paging.containsKey("next")) {
            		var next  = paging.get("next");
            		if(next!=null && !next.isBlank()) {
            			url = paging.get("next").toString();
            		}
            	}
            }
		}
		return returned;
	}
}