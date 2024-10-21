package org.ndx.aadarchi.technology.detector.model;

import org.ndx.aadarchi.technology.detector.exceptions.ExtractionException;

public class InvalidArtifactCoordinates extends ExtractionException {
    public InvalidArtifactCoordinates(String message) {
        super(message);
    }
}
