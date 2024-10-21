package org.ndx.aadarchi.technology.detector.pypi.exception;

public class CannotExecuteBigQuery extends PypiExtractionException {
  public CannotExecuteBigQuery(String message) {
    super(message);
  }

  public CannotExecuteBigQuery(String message, Throwable cause) {
    super(message, cause);
  }
}
