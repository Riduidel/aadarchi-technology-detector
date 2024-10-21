package org.ndx.aadarchi.technology.detector.pypi.exception;

import org.ndx.aadarchi.technology.detector.exceptions.ExtractionException;

public class PypiExtractionException extends ExtractionException {
    public PypiExtractionException(String message) {
        super(message);
    }

    public PypiExtractionException(String message, Throwable cause) {
        super(message, cause);
    }
}
