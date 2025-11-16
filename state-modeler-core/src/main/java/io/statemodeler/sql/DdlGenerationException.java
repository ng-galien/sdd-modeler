package io.statemodeler.sql;

/**
 * Exception thrown when DDL generation fails.
 */
public class DdlGenerationException extends Exception {

    public DdlGenerationException(String message) {
        super(message);
    }

    public DdlGenerationException(String message, Throwable cause) {
        super(message, cause);
    }

    public DdlGenerationException(Throwable cause) {
        super(cause);
    }
}
