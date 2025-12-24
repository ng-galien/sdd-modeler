package io.statemodeler.codegen;

import io.pebbletemplates.pebble.PebbleEngine;
import io.statemodeler.codegen.http.JavaHttpClientGenerator;
import io.statemodeler.codegen.java.*;
import io.statemodeler.core.SddModel;
import java.util.HashMap;
import java.util.Map;

/**
 * Pebble-based code generator implementation. Uses templates stored under
 * src/main/resources/templates.
 */
public class PebbleCodeGenerator implements CodeGenerator {

    private final PebbleEngine engine;
    private final String language;
    private final JavaContextBuilder javaContextBuilder;
    private final JavaCodeFormatter javaFormatter;

    public PebbleCodeGenerator(String language) {
        this.engine = new PebbleEngine.Builder().build();
        this.language = language;
        this.javaContextBuilder = new JavaContextBuilder();
        this.javaFormatter = new JavaCodeFormatter();
    }

    @Override
    public String getLanguage() {
        return language;
    }

    @Override
    public Map<String, String> generate(SddModel model) {
        Map<String, String> generatedFiles = new HashMap<>();

        if ("java".equals(language)) {
            var codegenConfig = model.database() != null ? model.database().codegenConfig() : null;
            generatedFiles.putAll(new JavaEntityGenerator(engine, javaContextBuilder).generate(model));
            generatedFiles.putAll(new JavaRepositoryGenerator(engine, javaContextBuilder).generate(model));
            generatedFiles.putAll(new JavaDtoGenerator(engine, javaContextBuilder).generate(model));
            if (codegenConfig == null || codegenConfig.generateController()) {
                generatedFiles.putAll(new JavaControllerGenerator(engine, javaContextBuilder).generate(model));
            }
            generatedFiles.putAll(new JavaServiceGenerator(engine, javaContextBuilder).generate(model));
            generatedFiles.putAll(new JavaConverterGenerator(engine, javaContextBuilder).generate(model));
            generatedFiles.putAll(new JavaConfigGenerator(engine, javaContextBuilder).generate(model));
            generatedFiles.putAll(new JavaHttpClientGenerator(engine, javaContextBuilder).generate(model));
            if (codegenConfig != null && codegenConfig.generateMcp()) {
                generatedFiles.putAll(new JavaMcpServerGenerator(engine, javaContextBuilder).generate(model));
                generatedFiles.putAll(new JavaMcpConfigGenerator(engine, javaContextBuilder).generate(model));
            }

            // Apply formatting to all Java files
            for (Map.Entry<String, String> entry : generatedFiles.entrySet()) {
                if (entry.getKey().endsWith(".java")) {
                    try {
                        entry.setValue(javaFormatter.format(entry.getValue()));
                    } catch (Exception e) {
                        System.err.println("Failed to format file: " + entry.getKey());
                        throw e;
                    }
                }
            }
        } else {
            // Fallback or other languages (not fully implemented in this refactor)
            // For now, we only support Java fully with the new structure.
            // If other languages are needed, similar specialized generators should be
            // created.
        }

        return generatedFiles;
    }
}
