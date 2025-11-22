package io.statemodeler.codegen.java;

import com.palantir.javaformat.java.Formatter;
import com.palantir.javaformat.java.FormatterException;

/**
 * Utility class to format Java code using Palantir Java Format.
 */
public class JavaCodeFormatter {

    private final Formatter formatter;

    public JavaCodeFormatter() {
        this.formatter = Formatter.create();
    }

    /**
     * Formats the given Java source code.
     *
     * @param source The Java source code to format.
     * @return The formatted source code, or the original source if formatting
     *         fails.
     */
    public String format(String source) {
        try {
            return formatter.formatSource(source);
        } catch (FormatterException e) {
            // If formatting fails (e.g., due to syntax errors in the generated code),
            // return the original source so the user can debug it.
            // We might want to log this properly in the future.
            System.err.println("Failed to format Java code: " + e.getMessage());
            return source;
        } catch (Throwable e) {
            System.err.println("Unexpected error during Java formatting: " + e.getMessage());
            e.printStackTrace();
            return source;
        }
    }
}
