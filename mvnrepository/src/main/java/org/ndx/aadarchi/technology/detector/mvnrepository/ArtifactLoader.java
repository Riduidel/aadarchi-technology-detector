package org.ndx.aadarchi.technology.detector.mvnrepository;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.io.IOUtils;

import com.microsoft.playwright.Page;

public abstract class ArtifactLoader {
	private static String artifactsUrlExtractor;
	
	static {
		try {
			artifactsUrlExtractor = IOUtils.toString(ArtifactLoader.class.getClassLoader().getResourceAsStream("artifacts_urls_extractor.js"), "UTF-8");
		} catch (IOException e) {
			throw new UnsupportedOperationException(String.format("Unable to load script from %s", 
					ArtifactLoader.class.getClassLoader().getResource("artifacts_urls_extractor.js")), e);
		}
	}

	public abstract Set<ArtifactInformations> loadArtifacts(Page page) throws IOException;

	protected static Set<ArtifactInformations> loadPageList(Page page, String url) {
		Set<ArtifactInformations> returned = new TreeSet<ArtifactInformations>();
		while(url!=null) {
			ExtractPopularMvnRepositoryArtifacts.logger.info(String.format("Loading page %s", url));
			page.navigate(url);
			Map pageInfos = ((Map) page.evaluate(artifactsUrlExtractor));

            if (!pageInfos.containsKey("data")) {
                ExtractPopularMvnRepositoryArtifacts.logger.warning(String.format("Page %s has empty data %s", url, pageInfos));
            } else {
            	List<Map<String, String>> data = (List) pageInfos.get("data");
            	for (Map<String, String> object : data) {
					returned.add(new ArtifactInformations(object.get("groupId"), object.get("artifactId")));
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