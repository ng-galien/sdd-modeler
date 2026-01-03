package io.statemodeler.codegen.java;

import com.palantir.javaformat.java.Formatter;
import com.palantir.javaformat.java.FormatterException;

/**
 * Utility class to format Java code using Palantir Java Format.
 */
public class JavaCodeFormatter {

    private final Formatter formatter;

    public JavaCodeFormatter() {
        openJavacModules();
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
        if (Boolean.getBoolean("sdd.disableFormatter")) {
            return source;
        }
        try {
            return formatter.formatSource(source);
        } catch (FormatterException e) {
            // If formatting fails (e.g., due to syntax errors in the generated code),
            // return the original source so the user can debug it.
            // We might want to log this properly in the future.
            System.err.println("Failed to format Java code: " + e.getMessage());

            // Save to file for debugging
            try {
                java.nio.file.Files.writeString(
                        java.nio.file.Path.of("failed-format.java"), source, java.nio.charset.StandardCharsets.UTF_8);
                System.err.println("Saved unformatted source to: failed-format.java");
            } catch (java.io.IOException ioEx) {
                // Ignore
            }

            return source;
        } catch (Throwable e) {
            System.err.println("Unexpected error during Java formatting: " + e.getMessage());
            e.printStackTrace();
            return source;
        }
    }

    private void openJavacModules() {
        try {
            Class<?> javacClass = Class.forName("com.sun.tools.javac.util.Context");
            Module compilerModule = javacClass.getModule();
            Module targetModule = JavaCodeFormatter.class.getModule();
            var addOpens = Module.class.getDeclaredMethod("addOpens", String.class, Module.class);
            addOpens.setAccessible(true);
            String[] packages = {
                "com.sun.tools.javac.api",
                "com.sun.tools.javac.code",
                "com.sun.tools.javac.file",
                "com.sun.tools.javac.parser",
                "com.sun.tools.javac.tree",
                "com.sun.tools.javac.util"
            };
            for (String pkg : packages) {
                addOpens.invoke(compilerModule, pkg, targetModule);
            }
        } catch (Throwable t) {
            System.err.println("Warning: unable to open javac modules for formatter: " + t.getMessage());
        }
    }
}
