package org.ndx.aadarchi.technology.detector.exception;

public class GitHubOperationException extends ExtractionException {
    public GitHubOperationException(String message) {
        super(message);
    }

    public GitHubOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
