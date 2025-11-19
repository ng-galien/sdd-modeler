package io.statemodeler.codegen;

import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import io.statemodeler.core.EntityDef;
import io.statemodeler.core.SddModel;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

/**
 * Pebble-based code generator implementation. Uses templates stored under src/main/resources/templates.
 */
public class PebbleCodeGenerator implements CodeGenerator {

    private final PebbleEngine engine;
    private final String language;

    public PebbleCodeGenerator(String language) {
        this.engine = new PebbleEngine.Builder().build();
        this.language = language;
    }

    @Override
    public String getLanguage() {
        return language;
    }

    @Override
    public Map<String, String> generate(SddModel model) {
        Map<String, String> generatedFiles = new HashMap<>();

        // Example: Generate a file per entity
        for (EntityDef entity : model.entities().values()) {
            String content = generateEntity(entity, model);
            String filename = resolveFilename(entity, model);
            generatedFiles.put(filename, content);
        }

        return generatedFiles;
    }

    private String generateEntity(EntityDef entity, SddModel model) {
        String templatePath = "templates/" + language + "/entity." + getExtension() + ".pebble";
        PebbleTemplate template = engine.getTemplate(templatePath);
        Map<String, Object> context = new HashMap<>();
        context.put("entity", entity);
        context.put("model", model);
        context.put("options", model.database() != null ? model.database().generatorOptions() : Map.of());

        Writer writer = new StringWriter();
        try {
            template.evaluate(writer, context);
            return writer.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate entity " + entity.name(), e);
        }
    }

    private String getExtension() {
        return switch (language) {
            case "java" -> "java";
            case "python" -> "py";
            default -> "txt";
        };
    }

    private String resolveFilename(EntityDef entity, SddModel model) {
        if ("java".equals(language)) {
            var options = model.database().generatorOptions();
            var pkg = options != null ? options.getOrDefault("packageName", "com.example") : "com.example";
            return pkg.replace('.', '/') + "/" + entity.name() + ".java";
        }
        return entity.name() + "." + getExtension();
    }
}
