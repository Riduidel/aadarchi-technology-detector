package org.ndx.aadarchi.technology.detector.mvnrepository.exception;

public class CannotRunMavenCommand extends MvnExtractionException {
    public CannotRunMavenCommand(String message) {
        super(message);
    }
    public CannotRunMavenCommand(String message, Throwable cause) {
        super(message, cause);
    }
}
