package org.ndx.aadarchi.technology.detector.history;

import org.ndx.aadarchi.technology.detector.exceptions.ExtractionException;

public class CannotCloneGitRepository extends ExtractionException {

	public CannotCloneGitRepository(String message, Throwable cause) {
		super(message, cause);
	}

}
