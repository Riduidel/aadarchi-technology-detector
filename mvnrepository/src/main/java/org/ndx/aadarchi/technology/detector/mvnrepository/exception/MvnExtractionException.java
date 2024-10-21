package org.ndx.aadarchi.technology.detector.mvnrepository.exception;

import org.ndx.aadarchi.technology.detector.exceptions.ExtractionException;

public class MvnExtractionException extends ExtractionException {
  public MvnExtractionException(String message) {
    super(message);
  }

  public MvnExtractionException(String message, Throwable cause) {
    super(message, cause);
  }
}
