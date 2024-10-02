package org.ndx.aadarchi.technology.detector.loader;

import java.nio.file.Path;

import org.kohsuke.github.GitHub;

/**
 * This interface allows easy communication of elements that will be needed
 * to get informations.
 * Typically, Playwright BrowserContext will be stored in this very context
 */
public interface ExtractionContext {
	/**
	 * @return path of the used cache folder
	 */
	Path getCache();
	/**
	 * Get github client
	 * @return
	 */
	GitHub getGithub();
}