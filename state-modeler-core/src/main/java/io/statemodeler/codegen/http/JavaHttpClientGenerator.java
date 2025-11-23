package io.statemodeler.codegen.http;

import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import io.statemodeler.codegen.java.JavaContextBuilder;
import io.statemodeler.core.EntityDef;
import io.statemodeler.core.SddModel;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

public class JavaHttpClientGenerator {

    private final PebbleEngine engine;
    private final JavaContextBuilder contextBuilder;

    public JavaHttpClientGenerator(PebbleEngine engine, JavaContextBuilder contextBuilder) {
        this.engine = engine;
        this.contextBuilder = contextBuilder;
    }

    public Map<String, String> generate(SddModel model) {
        Map<String, String> generatedFiles = new HashMap<>();
        for (EntityDef entity : model.entities().values()) {
            String content = generateHttpFile(entity, model);
            String filename = resolveHttpFilename(entity);
            generatedFiles.put(filename, content);
        }
        return generatedFiles;
    }

    private String generateHttpFile(EntityDef entity, SddModel model) {
        PebbleTemplate template = engine.getTemplate("templates/http/rest_api.http.pebble");
        Map<String, Object> context = new HashMap<>();
        Map<String, Object> entityCtx = contextBuilder.buildEntityContext(entity);
        context.put("entity", entityCtx);
        context.put("options", model.database() != null ? model.database().generatorOptions() : Map.of());

        Writer writer = new StringWriter();
        try {
            template.evaluate(writer, context);
            return writer.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate HTTP file for " + entity.name(), e);
        }
    }

    private String resolveHttpFilename(EntityDef entity) {
        return "http/" + contextBuilder.toCamel(entity.name()) + ".http";
    }
}
