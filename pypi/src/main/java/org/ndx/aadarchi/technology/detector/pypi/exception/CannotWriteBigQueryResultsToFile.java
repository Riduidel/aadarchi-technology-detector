package org.ndx.aadarchi.technology.detector.pypi.exception;

public class CannotWriteBigQueryResultsToFile extends PypiExtractionException {
    public CannotWriteBigQueryResultsToFile(String message, Throwable cause) {
        super(message, cause);
    }
}