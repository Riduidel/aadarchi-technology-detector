package org.ndx.aadarchi.technology.detector.model;

import org.ndx.aadarchi.technology.detector.exceptions.ExtractionException;

public class CannotLoadBadlyNamedArtifacts extends ExtractionException {
    public CannotLoadBadlyNamedArtifacts(String message, Throwable cause) {
        super(message, cause);
    }
}
