package org.ndx.aadarchi.technology.detector.npmjs.exception;

import org.ndx.aadarchi.technology.detector.exceptions.ExtractionException;

public class NpmjsExtractionException extends ExtractionException {
    public NpmjsExtractionException(String message) {
        super(message);
    }

    public NpmjsExtractionException(String message, Throwable cause) {
        super(message, cause);
    }
}
