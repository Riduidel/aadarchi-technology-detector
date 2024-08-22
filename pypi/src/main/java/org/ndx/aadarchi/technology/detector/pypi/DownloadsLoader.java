package org.ndx.aadarchi.technology.detector.pypi;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;

import org.ndx.aadarchi.technology.detector.helper.BasicArtifactLoader;
import org.ndx.aadarchi.technology.detector.model.ArtifactDetails;

public class DownloadsLoader extends BasicArtifactLoader<PypiContext> {

	private ArtifactDetails artifact;
	private String period;

	public DownloadsLoader(ArtifactDetails artifact, Path cache, String period) {
		registerCachedArtifacts(cache, 
				String.format("packages/%s/%s.json", period, artifact.getName()));
		this.artifact = artifact;
		this.period = period;
	}

	@Override
	public Collection<ArtifactDetails> doLoadArtifacts(PypiContext context) throws Exception {
		return Arrays.asList(context.countDownloadsOf(artifact, period));
	}

	public ArtifactDetails getDownloads(PypiContext context) throws Exception {
		return loadArtifacts(context).iterator().next();
	}

}
