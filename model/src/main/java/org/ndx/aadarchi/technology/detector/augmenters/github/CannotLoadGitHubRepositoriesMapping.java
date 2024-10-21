package org.ndx.aadarchi.technology.detector.augmenters.github;

import org.ndx.aadarchi.technology.detector.exceptions.ExtractionException;

public class CannotLoadGitHubRepositoriesMapping extends ExtractionException {
	public CannotLoadGitHubRepositoriesMapping(String message, Throwable cause) {
		super(message, cause);
	}
}
