package org.ndx.aadarchi.technology.detector.pypi.exception;

public class RateLimitReached extends PypiExtractionException {

	public RateLimitReached(String message) {
		super(message);
	}
}
