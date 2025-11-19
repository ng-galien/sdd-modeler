package io.statemodeler.codegen;

import io.statemodeler.core.SddModel;
import java.util.Map;

/**
 * Generic code generator interface to produce source files from an SDD model.
 */
public interface CodeGenerator {
    /**
     * Generates source code for the given SDD model.
     * @param model The SDD model to generate code for.
     * @return A map where key is the file path (relative to source root) and value is the file content.
     */
    Map<String, String> generate(SddModel model);

    /**
     * The language this generator targets (e.g., "java", "python").
     */
    String getLanguage();
}
