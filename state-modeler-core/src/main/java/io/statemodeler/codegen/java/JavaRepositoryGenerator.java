package io.statemodeler.codegen.java;

import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import io.statemodeler.core.EntityDef;
import io.statemodeler.core.SddModel;
import io.statemodeler.core.StateDef;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class JavaRepositoryGenerator {

    private final PebbleEngine engine;
    private final JavaContextBuilder contextBuilder;

    public JavaRepositoryGenerator(PebbleEngine engine, JavaContextBuilder contextBuilder) {
        this.engine = engine;
        this.contextBuilder = contextBuilder;
    }

    public Map<String, String> generate(SddModel model) {
        Map<String, String> generatedFiles = new HashMap<>();
        for (EntityDef entity : model.entities().values()) {
            for (StateDef state : entity.states().values()) {
                String content = generateRepository(entity, state, model);
                String filename = resolveRepositoryFilename(entity, state, model);
                generatedFiles.put(filename, content);
            }
        }
        return generatedFiles;
    }

    private String generateRepository(EntityDef entity, StateDef state, SddModel model) {
        PebbleTemplate template = engine.getTemplate("templates/java/repository.java.pebble");
        Map<String, Object> context = new HashMap<>();
        Map<String, Object> entityCtx = contextBuilder.buildEntityContext(entity);
        context.put("entity", entityCtx);
        context.put("state", contextBuilder.buildStateContext(state, entityCtx));
        Map<String, Object> modelCtx = contextBuilder.buildModelContext(model);
        context.put("model", modelCtx);

        Object modelImps = modelCtx.get("imports");
        Set<String> imports = new HashSet<>();
        if (modelImps instanceof Set<?> mis) {
            for (Object o : mis)
                if (o instanceof String str)
                    imports.add(str);
        }
        context.put("imports", imports);
        context.put("options", model.database() != null ? model.database().generatorOptions() : Map.of());

        Writer writer = new StringWriter();
        try {
            template.evaluate(writer, context);
            return writer.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate repository for " + state.name(), e);
        }
    }

    private String resolveRepositoryFilename(EntityDef entity, StateDef state, SddModel model) {
        var options = model.database().generatorOptions();
        var pkg = options != null ? options.getOrDefault("packageName", "com.example") : "com.example";
        return pkg.replace('.', '/') + "/" + contextBuilder.toPascal(state.name()) + "Repository.java";
    }
}
