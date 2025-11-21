package io.statemodeler.codegen;

/**
 * Factory for creating code generators for different languages.
 */
public final class CodeGenerators {

    private CodeGenerators() {
        // utility class
    }

    public static CodeGenerator forLanguage(String language) {
        return switch ((language == null) ? "" : language.toLowerCase()) {
            case "java" -> new PebbleCodeGenerator("java");
            default -> throw new IllegalArgumentException("Unsupported generation language: " + language);
        };
    }

    public static String[] getSupportedLanguages() {
        return new String[] {"java"};
    }

    public static boolean isSupported(String language) {
        try {
            forLanguage(language);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
