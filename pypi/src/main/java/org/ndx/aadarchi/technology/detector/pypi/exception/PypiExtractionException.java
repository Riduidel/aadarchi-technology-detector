package org.ndx.aadarchi.technology.detector.pypi.exception;

import org.ndx.aadarchi.technology.detector.exception.ExtractionException;

public class PypiExtractionException extends ExtractionException {
    public PypiExtractionException() {
        super();
    }
    public PypiExtractionException(String message) {
        super(message);
    }

    public PypiExtractionException(String message, Throwable cause) {
        super(message, cause);
    }

    public PypiExtractionException(Throwable cause) {
        super(cause);
    }
}
