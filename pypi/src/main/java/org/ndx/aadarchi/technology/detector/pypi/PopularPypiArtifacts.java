package org.ndx.aadarchi.technology.detector.pypi;

import java.net.http.HttpClient;
import java.nio.file.Path;
import java.util.Collection;

import org.ndx.aadarchi.technology.detector.helper.BasicArtifactLoader;
import org.ndx.aadarchi.technology.detector.model.ArtifactDetails;

public class PopularPypiArtifacts extends BasicArtifactLoader<PypiContext>{

	public PopularPypiArtifacts(Path cache, HttpClient client) {
		registerCachedArtifacts(cache, "popular_pypi_artifacts.json");
	}

	@Override
	public Collection<ArtifactDetails> doLoadArtifacts(PypiContext context) throws Exception {
		return context.loadPopularArtifacts();
	}
}
