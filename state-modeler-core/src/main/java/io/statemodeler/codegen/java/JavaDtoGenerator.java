package io.statemodeler.codegen.java;

import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import io.statemodeler.core.EntityDef;
import io.statemodeler.core.SddModel;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class JavaDtoGenerator {

    private final PebbleEngine engine;
    private final JavaContextBuilder contextBuilder;

    public JavaDtoGenerator(PebbleEngine engine, JavaContextBuilder contextBuilder) {
        this.engine = engine;
        this.contextBuilder = contextBuilder;
    }

    public Map<String, String> generate(SddModel model) {
        Map<String, String> generatedFiles = new HashMap<>();
        for (EntityDef entity : model.entities().values()) {
            String content = generateDto(entity, model);
            String filename = resolveDtoFilename(entity, model);
            generatedFiles.put(filename, content);
        }
        return generatedFiles;
    }

    private String generateDto(EntityDef entity, SddModel model) {
        PebbleTemplate template = engine.getTemplate("templates/java/dto.java.pebble");
        Map<String, Object> context = new HashMap<>();
        Map<String, Object> entityCtx = contextBuilder.buildEntityContext(entity);
        context.put("entity", entityCtx);
        Map<String, Object> modelCtx = contextBuilder.buildModelContext(model);
        context.put("model", modelCtx);

        Set<String> imports = new HashSet<>();
        Object modelImps = modelCtx.get("imports");
        if (modelImps instanceof Set<?> mis) {
            for (Object o : mis) if (o instanceof String str) imports.add(str);
        }
        Object entityImps = entityCtx.get("imports");
        if (entityImps instanceof Set<?> eis) {
            for (Object o : eis) if (o instanceof String str) imports.add(str);
        }
        context.put("imports", imports);
        context.put("options", model.database() != null ? model.database().generatorOptions() : Map.of());

        Writer writer = new StringWriter();
        try {
            template.evaluate(writer, context);
            return writer.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate DTO for " + entity.name(), e);
        }
    }

    private String resolveDtoFilename(EntityDef entity, SddModel model) {
        var options = model.database().generatorOptions();
        var pkg = options != null ? options.getOrDefault("packageName", "com.example") : "com.example";
        return pkg.replace('.', '/') + "/" + contextBuilder.toPascal(entity.name()) + "Dto.java";
    }
}
