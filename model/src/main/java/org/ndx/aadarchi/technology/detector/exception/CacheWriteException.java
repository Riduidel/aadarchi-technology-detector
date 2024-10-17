package org.ndx.aadarchi.technology.detector.exception;

public class CacheWriteException extends ExtractionException {
    public CacheWriteException(String message) {
        super(message);
    }

    public CacheWriteException(String message, Throwable cause) {
        super(message, cause);
    }
}
