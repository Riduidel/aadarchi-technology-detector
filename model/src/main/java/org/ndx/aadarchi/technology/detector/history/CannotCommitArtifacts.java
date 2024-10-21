package org.ndx.aadarchi.technology.detector.history;

import org.ndx.aadarchi.technology.detector.exceptions.ExtractionException;

public class CannotCommitArtifacts extends ExtractionException {
	public CannotCommitArtifacts(String message, Throwable cause) {
		super(message, cause);
	}
}
