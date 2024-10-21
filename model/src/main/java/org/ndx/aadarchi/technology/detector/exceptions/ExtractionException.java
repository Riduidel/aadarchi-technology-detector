package org.ndx.aadarchi.technology.detector.exceptions;

import java.io.Serial;

public abstract class ExtractionException extends RuntimeException  {
	public ExtractionException(String message) {
		super(message);
	}

	public ExtractionException(String message, Throwable cause) {
        super(message, cause);
    }
}
