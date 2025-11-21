package io.statemodeler.codegen.java;

import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import io.statemodeler.core.SddModel;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

public class JavaConfigGenerator {

    private final PebbleEngine engine;
    private final JavaContextBuilder contextBuilder;

    public JavaConfigGenerator(PebbleEngine engine, JavaContextBuilder contextBuilder) {
        this.engine = engine;
        this.contextBuilder = contextBuilder;
    }

    public Map<String, String> generate(SddModel model) {
        Map<String, String> generatedFiles = new HashMap<>();
        String content = generateConfiguration(model);
        String filename = resolveConfigurationFilename(model);
        generatedFiles.put(filename, content);
        return generatedFiles;
    }

    private String generateConfiguration(SddModel model) {
        PebbleTemplate template = engine.getTemplate("templates/java/configuration.java.pebble");
        Map<String, Object> context = new HashMap<>();
        context.put("model", contextBuilder.buildModelContext(model));
        context.put("options", model.database() != null ? model.database().generatorOptions() : Map.of());

        Writer writer = new StringWriter();
        try {
            template.evaluate(writer, context);
            return writer.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Configuration", e);
        }
    }

    private String resolveConfigurationFilename(SddModel model) {
        var options = model.database().generatorOptions();
        var pkg = options != null ? options.getOrDefault("packageName", "com.example") : "com.example";
        return pkg.replace('.', '/') + "/SddConfig.java";
    }
}
