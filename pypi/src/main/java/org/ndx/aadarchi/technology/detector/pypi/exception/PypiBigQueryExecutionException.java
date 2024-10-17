package org.ndx.aadarchi.technology.detector.pypi.exception;

public class PypiBigQueryExecutionException extends PypiExtractionException {
  public PypiBigQueryExecutionException(String message) {
    super(message);
  }

  public PypiBigQueryExecutionException(String message, Throwable cause) {
    super(message, cause);
  }
}
