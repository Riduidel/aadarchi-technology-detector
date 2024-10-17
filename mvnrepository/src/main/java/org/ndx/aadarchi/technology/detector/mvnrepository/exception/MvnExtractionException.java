package org.ndx.aadarchi.technology.detector.mvnrepository.exception;

import org.ndx.aadarchi.technology.detector.exception.ExtractionException;

public class MvnExtractionException extends ExtractionException {
  public MvnExtractionException() {
    super();
  }
  public MvnExtractionException(String message) {
    super(message);
  }

  public MvnExtractionException(String message, Throwable cause) {
    super(message, cause);
  }

  public MvnExtractionException(Throwable cause) {
    super(cause);
  }
}
