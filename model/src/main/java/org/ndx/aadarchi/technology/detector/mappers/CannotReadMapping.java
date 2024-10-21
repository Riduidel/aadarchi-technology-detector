package org.ndx.aadarchi.technology.detector.mappers;

import org.ndx.aadarchi.technology.detector.exceptions.ExtractionException;

public class CannotReadMapping extends ExtractionException {
    public CannotReadMapping(String message, Throwable cause) {
        super(message, cause);
    }
}
