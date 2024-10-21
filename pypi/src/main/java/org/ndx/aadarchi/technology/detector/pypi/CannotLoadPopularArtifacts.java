package org.ndx.aadarchi.technology.detector.pypi;

import org.ndx.aadarchi.technology.detector.pypi.exception.PypiExtractionException;

public class CannotLoadPopularArtifacts extends PypiExtractionException {
	public CannotLoadPopularArtifacts(String message, Throwable cause) {
		super(message, cause);
	}

}
