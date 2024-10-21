package org.ndx.aadarchi.technology.detector.history;

import org.ndx.aadarchi.technology.detector.exceptions.ExtractionException;

public class CannotGenerateGitHistory extends ExtractionException {

	public CannotGenerateGitHistory(String message, Throwable cause) {
		super(message, cause);
	}

}
