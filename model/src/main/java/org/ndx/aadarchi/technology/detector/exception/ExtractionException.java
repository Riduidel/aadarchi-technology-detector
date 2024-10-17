package org.ndx.aadarchi.technology.detector.exception;

import java.io.Serial;

public abstract class ExtractionException extends RuntimeException  {
    @Serial
    private static final long serialVersionUID = -8460356990632230194L;

    public ExtractionException() {
        super();
    }

    public ExtractionException(String message, Throwable cause) {
        super(message, cause);
    }

    public ExtractionException(String message) {
        super(message);
    }

    public ExtractionException(Throwable cause) {
        super(cause);
    }
}
