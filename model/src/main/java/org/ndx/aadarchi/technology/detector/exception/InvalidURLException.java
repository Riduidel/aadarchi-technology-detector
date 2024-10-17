package org.ndx.aadarchi.technology.detector.exception;

public class InvalidURLException extends ExtractionException {
    public InvalidURLException(String message) {
        super(message);
    }

    public InvalidURLException(String message, Throwable cause) {
        super(message, cause);
    }
}
