package org.ndx.aadarchi.technology.detector.augmenters.github;

import org.ndx.aadarchi.technology.detector.exceptions.ExtractionException;

public class CannotFetchStargazers extends ExtractionException {
	public CannotFetchStargazers(String message, Throwable cause) {
		super(message, cause);
	}
}
