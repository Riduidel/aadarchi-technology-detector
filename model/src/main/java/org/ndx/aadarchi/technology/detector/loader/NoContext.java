package org.ndx.aadarchi.technology.detector.loader;

import java.io.IOException;
import java.util.Optional;

import org.kohsuke.github.GitHub;

/**
 * An empty context, for extractors with no special needs
 */
public class NoContext implements ExtractionContext {
	Optional<GitHub> github = Optional.empty();

	@Override
	public GitHub getGithub() {
		if (github.isEmpty()) {
			try {
				github = Optional.of(GitHub.connect());
			} catch (IOException e) {
				throw new RuntimeException("Cannot connect to GitHub", e);
			}
		}
		return github.get();
	}

}
