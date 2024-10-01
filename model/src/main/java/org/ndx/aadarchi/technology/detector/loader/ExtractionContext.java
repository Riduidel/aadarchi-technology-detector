package org.ndx.aadarchi.technology.detector.loader;

import org.kohsuke.github.GitHub;

/**
 * This interface allows easy communication of elements that will be needed
 * to get informations.
 * Typically, Playwright BrowserContext will be stored in this very context
 */
public interface ExtractionContext {
	/**
	 * Get github client
	 * @return
	 */
	GitHub getGithub();
}