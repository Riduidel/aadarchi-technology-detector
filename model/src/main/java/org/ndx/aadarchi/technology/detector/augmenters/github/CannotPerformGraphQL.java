package org.ndx.aadarchi.technology.detector.augmenters.github;

import org.ndx.aadarchi.technology.detector.exceptions.ExtractionException;

public class CannotPerformGraphQL extends ExtractionException {

	public CannotPerformGraphQL(String message, Throwable cause) {
		super(message, cause);
	}

}
