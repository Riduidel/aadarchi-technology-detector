package org.ndx.aadarchi.technology.detector.helper;

import org.ndx.aadarchi.technology.detector.exceptions.ExtractionException;

public class InvalidURLException extends ExtractionException {
    public InvalidURLException(String message, Throwable cause) {
        super(message, cause);
    }
}
