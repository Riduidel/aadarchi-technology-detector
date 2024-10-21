package org.ndx.aadarchi.technology.detector.augmenters.github;

import org.ndx.aadarchi.technology.detector.exceptions.ExtractionException;

public class CannotGetStargazers extends ExtractionException {
	public CannotGetStargazers(String message, Throwable cause) {
		super(message, cause);
	}
}
