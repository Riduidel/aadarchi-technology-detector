package org.ndx.aadarchi.technology.detector.mvnrepository.exception;

public class MvnExecutionException extends MvnExtractionException {
    public MvnExecutionException(String message) {
        super(message);
    }
    public MvnExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
