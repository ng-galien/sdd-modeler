package io.statemodeler.dsl;

import lombok.experimental.StandardException;

/**
 * Exception thrown when model parsing fails due to invalid content or structure.
 * Follows modern Java exception practices with runtime semantics.
 */
@StandardException
public class ModelParsingException extends RuntimeException {}
