package io.statemodeler.validation;

/**
 * Factory for creating ModelValidator instances.
 * Provides convenient access to validation functionality.
 */
public final class ModelValidators {

    private ModelValidators() {
        // Utility class
    }

    /**
     * Create a new ModelValidator instance.
     *
     * @return a new DefaultModelValidator instance
     */
    public static DefaultModelValidator create() {
        return new DefaultModelValidator();
    }

    /**
     * Get a shared instance of the ModelValidator.
     * Thread-safe and suitable for reuse.
     *
     * @return the validator instance
     */
    public static DefaultModelValidator getInstance() {
        return ValidatorHolder.INSTANCE;
    }

    private static final class ValidatorHolder {
        private static final DefaultModelValidator INSTANCE = new DefaultModelValidator();
    }
}
