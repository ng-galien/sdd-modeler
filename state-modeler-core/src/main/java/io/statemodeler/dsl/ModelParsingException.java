package io.statemodeler.dsl;

/**
 * Exception thrown when parsing an SDD model fails.
 */
public class ModelParsingException extends Exception {

    public ModelParsingException(String message) {
        super(message);
    }

    public ModelParsingException(String message, Throwable cause) {
        super(message, cause);
    }
}
