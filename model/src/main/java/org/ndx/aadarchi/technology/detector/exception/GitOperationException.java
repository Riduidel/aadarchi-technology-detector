package org.ndx.aadarchi.technology.detector.exception;

public class GitOperationException extends ExtractionException {
    public GitOperationException(String message) {
        super(message);
    }

    public GitOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
