package org.ndx.aadarchi.technology.detector.helper;

import org.ndx.aadarchi.technology.detector.exceptions.ExtractionException;

public class CannotInitializeGitHubClient extends ExtractionException {
	public CannotInitializeGitHubClient(String message, Throwable cause) {
		super(message, cause);
	}
}
