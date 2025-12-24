package io.statemodeler.codegen.java;

import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import io.statemodeler.core.SddModel;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

public class JavaMcpConfigGenerator {

    private final PebbleEngine engine;
    private final JavaContextBuilder contextBuilder;

    public JavaMcpConfigGenerator(PebbleEngine engine, JavaContextBuilder contextBuilder) {
        this.engine = engine;
        this.contextBuilder = contextBuilder;
    }

    public Map<String, String> generate(SddModel model) {
        Map<String, String> generatedFiles = new HashMap<>();
        String content = generateConfiguration(model);
        generatedFiles.put("resources/application-mcp.properties", content);
        return generatedFiles;
    }

    private String generateConfiguration(SddModel model) {
        PebbleTemplate template = engine.getTemplate("templates/java/mcp_config.properties.pebble");
        Map<String, Object> context = new HashMap<>();
        context.put("model", model);
        context.put("modelCtx", contextBuilder.buildModelContext(model));
        context.put("options", model.database() != null ? model.database().generatorOptions() : Map.of());
        context.put("codegen", model.database() != null ? model.database().codegenConfig() : null);

        Writer writer = new StringWriter();
        try {
            template.evaluate(writer, context);
            return writer.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate MCP configuration", e);
        }
    }
}
