package org.ndx.aadarchi.technology.detector.mappers;

import org.ndx.aadarchi.technology.detector.exceptions.ExtractionException;

public class CannotWriteMapping extends ExtractionException {
    public CannotWriteMapping(String message, Throwable cause) {
        super(message, cause);
    }
}