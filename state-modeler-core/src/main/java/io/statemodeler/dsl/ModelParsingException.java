package io.statemodeler.dsl;

/**
 * Exception thrown when model parsing fails due to invalid content or structure.
 * Follows modern Java exception practices with runtime semantics.
 */
public class ModelParsingException extends RuntimeException {

    public ModelParsingException(String message) {
        super(message);
    }

    public ModelParsingException(String message, Throwable cause) {
        super(message, cause);
    }

    public ModelParsingException(Throwable cause) {
        super(cause);
    }
}
