package org.ndx.aadarchi.technology.detector.loader;

import java.io.IOException;
import java.nio.file.Path;

/**
 * An empty context, for extractors with no special needs
 */
public class NoContext extends AbstractContext implements ExtractionContext {
	public NoContext(Path cache, String githubToken) throws IOException {
		super(cache, githubToken);
	}
}
