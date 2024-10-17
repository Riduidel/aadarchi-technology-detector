package org.ndx.aadarchi.technology.detector.npmjs.exception;

import org.ndx.aadarchi.technology.detector.exception.ExtractionException;

public class NpmjsExtractionException extends ExtractionException {
    public NpmjsExtractionException() {
        super();
    }
    public NpmjsExtractionException(String message) {
        super(message);
    }

    public NpmjsExtractionException(String message, Throwable cause) {
        super(message, cause);
    }

    public NpmjsExtractionException(Throwable cause) {
        super(cause);
    }
}
